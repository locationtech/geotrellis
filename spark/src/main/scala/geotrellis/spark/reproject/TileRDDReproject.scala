/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.reproject

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.buffer._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd._
import org.apache.spark._

import scala.reflect.ClassTag

object TileRDDReproject {
  import Reproject.Options

  @transient private lazy val logger = LazyLogging(this)

  /** Reproject a set of buffered
    * @tparam           K           Key type; requires spatial component.
    * @tparam           V           Tile type; requires the ability to stitch, crop, reproject, merge, and create.
    *
    * @param            bufferedTiles      An RDD of buffered tiles, created using the BufferTiles operation.
    * @param            metadata           The raster metadata for this keyed tile set.
    * @param            destCrs            The CRS to reproject to.
    * @param            targetLayout       Either the layout scheme or layout definition to use when re-keying the reprojected layers.
    * @param            options            Reprojection options.
    *
    * @return           The new zoom level and the reprojected keyed tile RDD.
    */
  def apply[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    bufferedTiles: RDD[(K, BufferedTile[V])],
    metadata: TileLayerMetadata[K],
    destCrs: CRS,
    targetLayout: Either[LayoutScheme, LayoutDefinition],
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    val crs: CRS = metadata.crs
    val layout = metadata.layout
    val tileLayout: TileLayout = layout.tileLayout
    val sc = bufferedTiles.context

    val rasterReprojectOptions =
      options.rasterReprojectOptions.parentGridExtent match {
        case Some(_) =>
          // Assume caller knows what she/he is doing
          options.rasterReprojectOptions
        case None =>
          if(options.matchLayerExtent) {
            val parentGridExtent = ReprojectRasterExtent(layout: GridExtent, crs, destCrs, options.rasterReprojectOptions)
            options.rasterReprojectOptions.copy(parentGridExtent = Some(parentGridExtent))
          } else {
            options.rasterReprojectOptions
          }
      }

    val reprojectedTiles =
      bufferedTiles
        .mapPartitions { partition =>
          val transform = Transform(crs, destCrs)
          val inverseTransform = Transform(destCrs, crs)

          partition.map { case (key, BufferedTile(tile, gridBounds)) =>
            val innerExtent = key.getComponent[SpatialKey].extent(layout)
            val innerRasterExtent = RasterExtent(innerExtent, gridBounds.width, gridBounds.height)
            val outerGridBounds =
              GridBounds(
                -gridBounds.colMin,
                -gridBounds.rowMin,
                tile.cols - gridBounds.colMin - 1,
                tile.rows - gridBounds.rowMin - 1
              )
            val outerExtent = innerRasterExtent.extentFor(outerGridBounds, clamp = false)

            val window =
              if(options.matchLayerExtent) {
                gridBounds
              } else {
                // Reproject extra cells that are half the buffer size, as to avoid
                // any missed cells between tiles.
                GridBounds(
                  gridBounds.colMin / 2,
                  gridBounds.rowMin / 2,
                  (tile.cols + gridBounds.colMax - 1) / 2,
                  (tile.rows + gridBounds.rowMax - 1) / 2
                )
              }

            val Raster(newTile, newExtent) =
              tile.reproject(outerExtent, window, transform, inverseTransform, rasterReprojectOptions)

            ((key, newExtent), newTile)
          }
        }

    // no matter what we inspect the change in spatial extent to get the pixel counts
    val reprojectSummary = matchReprojectRasterExtent(
      metadata.crs, destCrs,
      metadata.layout,
      metadata.bounds.toOption.map { case KeyBounds(s, e) =>
        KeyBounds(s.getComponent[SpatialKey], e.getComponent[SpatialKey])
      })(sc)
    logger.info(s"$reprojectSummary")

    val (extent, cellSize) = options.rasterReprojectOptions.targetCellSize match {
      case Some(cs) =>
        val dataRasterExtent: RasterExtent = metadata.layout.createAlignedRasterExtent(metadata.extent)
        val targetRasterExtent = ReprojectRasterExtent(dataRasterExtent, metadata.crs, destCrs)
        logger.info(s"Target CellSize: $cs")
        (targetRasterExtent.extent, cs)

      case None =>
        (reprojectSummary.extent, reprojectSummary.cellSize)
    }

    val (zoom, newMetadata, tilerResampleMethod) =
      targetLayout match {
        case Left(layoutScheme) =>
          val LayoutLevel(z, layout) = layoutScheme.levelFor(extent, cellSize)
          val kb = metadata.bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
          val m = TileLayerMetadata(metadata.cellType, layout, extent, destCrs, kb)

          layoutScheme match {
            case _: FloatingLayoutScheme =>
              (z, m, NearestNeighbor)
            case _ =>
              (z, m, options.rasterReprojectOptions.method)
          }

        case Right(layoutDefinition) =>
          val kb = metadata.bounds.setSpatialBounds(KeyBounds(layoutDefinition.mapTransform(extent)))
          val m = TileLayerMetadata(metadata.cellType, layoutDefinition, extent, destCrs, kb)

          (0, m, options.rasterReprojectOptions.method)
      }

    // account for changes due to target layout, may be snapping higher or lower resolution
    val pixelRatio = reprojectSummary.rescaledPixelRatio(newMetadata.layout.cellSize)

    val part: Option[Partitioner] = if (pixelRatio > 1.2) {
      val newPartitionCount = (bufferedTiles.partitions.length * pixelRatio).toInt
      logger.info(s"Layout change grows potential number of tiles by $pixelRatio times, resizing to $newPartitionCount partitions.")
      Some(new HashPartitioner(partitions = newPartitionCount))
    } else None

    val tiled = reprojectedTiles.tileToLayout(newMetadata,
      Tiler.Options(resampleMethod = tilerResampleMethod, partitioner = bufferedTiles.partitioner))

    (zoom, ContextRDD(tiled, newMetadata))
  }

  /** Reproject a keyed tile RDD.
    *
    * @tparam           K           Key type; requires spatial component.
    * @tparam           V           Tile type; requires the ability to stitch, crop, reproject, merge, and create.
    *
    * @param            rdd                The keyed tile RDD.
    * @param            destCrs            The CRS to reproject to.
    * @param            targetLayout       The layout scheme to use when re-keying the reprojected layers.
    * @param            options            Reprojection options.
    *
    * @return           The new zoom level and the reprojected keyed tile RDD.
    */
  def apply[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    destCrs: CRS,
    targetLayout: Either[LayoutScheme, LayoutDefinition],
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    if(rdd.metadata.crs == destCrs) {
      val layout = rdd.metadata.layout

      val (zoom, bail) =
        targetLayout match {
          case Left(layoutScheme) =>
            val LayoutLevel(zoom, newLayout) = layoutScheme.levelFor(layout.extent, layout.cellSize)
            (zoom, newLayout == layout)
          case Right(layoutDefinition) =>
            (0, layoutDefinition == layout)
        }

      if(bail) {
        // This is a no-op, just return the source
        (zoom, rdd)
      } else {
        // We are tiling against a new layout but we
        // don't need to worry about buffers since the source and target are
        // in the same CRS.
        apply(rdd, destCrs, targetLayout, bufferSize = 0, options = options)
      }
    } else {
      val crs = rdd.metadata.crs
      val layout = rdd.metadata.layout
      val tileLayout = rdd.metadata.layout.tileLayout
      // Avoid capturing instantiated Transform in closure because its not Serializable
      lazy val transform = Transform(crs, destCrs)

      val bufferedTiles = BufferTiles(
        layer = rdd,
        includeKey = rdd.metadata.bounds.includes(_: K),
        getBufferSizes = { key: K =>
          val extent = key.getComponent[SpatialKey].extent(layout)
          val srcRE = RasterExtent(extent, tileLayout.tileCols, tileLayout.tileRows)
          val dstRE = ReprojectRasterExtent(srcRE, transform)

          // Reproject the extent back into the original CRS,
          // to determine how many border pixels we need.
          // Pad by one extra pixel.
          val e = dstRE.extent.reproject(destCrs, crs)
          val gb = srcRE.gridBoundsFor(e, clamp = false)
          BufferSizes(
            left   = 1 + (if(gb.colMin < 0) -gb.colMin else 0),
            right  = 1 + (if(gb.colMax >= srcRE.cols) gb.colMax - (srcRE.cols - 1) else 0),
            top    = 1 + (if(gb.rowMin < 0) -gb.rowMin else 0),
            bottom = 1 + (if(gb.rowMax >= srcRE.rows) gb.rowMax - (srcRE.rows - 1) else 0)
          )
        })

      apply(bufferedTiles, rdd.metadata, destCrs, targetLayout, options)
    }
  }

  /** Reproject this keyed tile RDD, using a constant border size for the operation.
    * @tparam           K                  Key type; requires spatial component.
    * @tparam           V                  Tile type; requires the ability to stitch, crop, reproject, merge, and create.
    *
    * @param            rdd                The keyed tile RDD.
    * @param            destCrs            The CRS to reproject to.
    * @param            targetLayout       The layout scheme to use when re-keying the reprojected layers.
    * @param            bufferSize         Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                      any side if there is an adjacent, abutting tile to contribute the border pixels.
    * @param            options            Reprojection options.
    *
    * @return           The new zoom level and the reprojected keyed tile RDD.
    *
    * @note             This is faster than computing the correct border size per key, so if you know that a specific border size will be sufficient
    *                   to be accurate, e.g. if the CRS's are not very different and so the rasters will not skew heavily, then this method can be used
    *                   for performance benefit.
    */
  def apply[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    destCrs: CRS,
    targetLayout: Either[LayoutScheme, LayoutDefinition],
    bufferSize: Int,
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    if(bufferSize == 0) {
      val fakeBuffers: RDD[(K, BufferedTile[V])] = rdd.withContext(_.mapValues { tile: V => BufferedTile(tile, GridBounds(0, 0, tile.cols - 1, tile.rows - 1)) })
      apply(fakeBuffers, rdd.metadata, destCrs, targetLayout, options)
    } else
      apply(rdd.bufferTiles(bufferSize), rdd.metadata, destCrs, targetLayout, options)


  /** Match pixel resolution between two layouts in different projections such that
    * cell size in target projection and layout matches the most resolute tile.
    *
    * Note: that the ammount of pixel resolution distortion during the reprojection
    * depends on the location of the extent being reprojeected.
    */
  private
  def matchReprojectRasterExtent(
    src: CRS, dst: CRS,
    layout: LayoutDefinition,
    keyBounds: Option[KeyBounds[SpatialKey]]
  )(implicit sc: SparkContext): ReprojectSummary = {
    // Bounds of tiles we need to examine
    val bounds: GridBounds = keyBounds match {
      case Some(kb) =>
        kb.toGridBounds
      case None =>
        GridBounds(0, 0, layout.layoutCols, layout.layoutRows)
    }

    val mapTransform = layout.mapTransform
    val getRasterExtent: (Int, Int) => RasterExtent = { (col, row) =>
      RasterExtent(mapTransform(col, row), layout.tileCols, layout.tileRows)
    }

    val chunks = bounds.split(512, 512).toVector
    sc.parallelize(chunks, chunks.length)
      .map { boundsChunk =>
        import scala.concurrent._
        import scala.concurrent.duration._
        import ExecutionContext.Implicits.global

        val splitWork: Iterator[Future[ReprojectSummary]] =
          boundsChunk.split(128,128) // large enough to be worth a Future
            .map { subChunk =>
              Future {
                // Proj4Transform is not thread safe
                val transform = Proj4Transform(src, dst)
                subChunk.coordsIter
                  .map { case (col, row) =>
                    val source = getRasterExtent(col, row)
                    val target = ReprojectRasterExtent(source, transform)
                    ReprojectSummary(
                      sourcePixels = source.size,
                      pixels = target.size,
                      extent = target.extent,
                      cellSize = target.cellSize
                    )
                  }
                  .reduce(_ combine _)
              }
            }

        Await
          .result(Future.sequence(splitWork), Duration.Inf)
          .reduce(_ combine _)
      }
      .reduce(_ combine _)
  }

  private case class ReprojectSummary(
    sourcePixels: Double,
    pixels: Double,
    extent: Extent,
    cellSize: CellSize
  ) {
    /** Combine summary and project pixel counts to highest resolution */
    def combine(other: ReprojectSummary): ReprojectSummary = {
      if (cellSize.resolution <= other.cellSize.resolution)
        ReprojectSummary(
          sourcePixels + other.sourcePixels,
          pixels + other.rescaledPixelCount(cellSize),
          extent combine other.extent,
          cellSize)
      else
        ReprojectSummary(
          sourcePixels + other.sourcePixels,
          rescaledPixelCount(other.cellSize) + other.pixels,
          extent combine other.extent,
          other.cellSize)
    }

    /** How many pixels were added in reproject */
    def pixelRatio: Double = pixels / sourcePixels

    def rescaledPixelRatio(target: CellSize): Double =
      rescaledPixelCount(target) / sourcePixels

    /** How many pixels would it take to cover the same area in different cellSize? */
    def rescaledPixelCount(target: CellSize): Double = {
      val CellSize(w0, h0) = cellSize
      val CellSize(w1, h1) = target
      pixels * (w0 * h0) / (w1 * h1)
    }


  }
}
