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
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.buffer.{BufferSizes, BufferedTile}
import geotrellis.raster.crop._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.raster.stitch._
import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.spark._
import geotrellis.spark.buffer.BufferTilesRDD
import geotrellis.spark.merge._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.spark.rdd._
import org.apache.spark._

import scala.reflect.ClassTag

object TileRDDReproject extends LazyLogging {
  import Reproject.Options

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
    V <: CellGrid[Int]: ClassTag: RasterRegionReproject: (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    bufferedTiles: RDD[(K, BufferedTile[V])],
    metadata: TileLayerMetadata[K],
    destCrs: CRS,
    targetLayout: Either[LayoutScheme, LayoutDefinition],
    options: Options,
    partitioner: Option[Partitioner]
  ): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    val crs: CRS = metadata.crs
    val layout = metadata.layout
    val tileLayout: TileLayout = layout.tileLayout
    implicit val sc = bufferedTiles.context

    val sourceDataGridExtent = metadata.layout.createAlignedGridExtent(metadata.extent)
    val passthroughGridExtent = ReprojectRasterExtent(sourceDataGridExtent, metadata.crs, destCrs)
    val targetDataExtent = passthroughGridExtent.extent

    val targetPartitioner: Option[Partitioner] = partitioner.orElse(bufferedTiles.partitioner)

    // inspect the change in spatial extent to get the pixel counts
    val reprojectSummary = matchReprojectRasterExtent(
      metadata.crs, destCrs,
      metadata.layout,
      metadata.bounds.toOption.map { case KeyBounds(s, e) =>
        KeyBounds(s.getComponent[SpatialKey], e.getComponent[SpatialKey])
      })
    logger.debug(s"$reprojectSummary")

    // First figure out where we're going through option yoga
    // You'll want to read [[ReprojectRasterExtent]] to grok this
    val LayoutLevel(targetZoom, targetLayerLayout) = targetLayout match {
      case Right(layoutDefinition) =>
        // A LayoutDefinition specifies a GridExtent.  The presence of this
        // option indicates that the user knows exactly the grid to resample to.
        LayoutLevel(0, layoutDefinition)

      case Left(layoutScheme: FloatingLayoutScheme) =>
        // FloatingLayoutScheme implies trying to match the resulting layout to
        // the extent of the reprojected input region.  This may require snapping
        // to a different GridExtent depending on the settings in
        // rasterReprojectOptions.
        if (options.matchLayerExtent) {
          val tre = ReprojectRasterExtent(layout, crs, destCrs, options.rasterReprojectOptions)

          layoutScheme.levelFor(tre.extent, tre.cellSize)
        } else {
          options.rasterReprojectOptions.parentGridExtent match {
            case Some(ge) =>
              layoutScheme.levelFor(targetDataExtent, ge.cellSize)

            case None =>
              options.rasterReprojectOptions.targetCellSize match {
                case Some(ct) =>
                  layoutScheme.levelFor(targetDataExtent, ct)

                case None =>
                  layoutScheme.levelFor(targetDataExtent, passthroughGridExtent.cellSize)
              }
          }
        }

      case Left(layoutScheme) =>
        // Zoomed or user-defined layout.  Cannot snap to new grid.  Only need
        // to find appropriate zoom level.
        if (options.matchLayerExtent) {
          val tre = ReprojectRasterExtent(
            sourceDataGridExtent, crs, destCrs,
            options.rasterReprojectOptions.copy(
              parentGridExtent=None, targetCellSize=None, targetRasterExtent=None))

          layoutScheme.levelFor(tre.extent, tre.cellSize)
        } else {
          val tre = ReprojectRasterExtent(
            sourceDataGridExtent, crs, destCrs,
            options.rasterReprojectOptions)

          if (options.rasterReprojectOptions.targetCellSize.isDefined
              || options.rasterReprojectOptions.parentGridExtent.isDefined) {
            // options targetCellSize or parentGridExtent will have effected cellSize
            layoutScheme.levelFor(tre.extent, tre.cellSize)
          } else {
            val cellSize: CellSize = reprojectSummary.cellSize
            layoutScheme.levelFor(tre.extent, cellSize)
          }
        }
    }

    val rasterReprojectOptions = options.rasterReprojectOptions.copy(
      parentGridExtent = Some(targetLayerLayout),
      targetCellSize = None,
      targetRasterExtent = None
    )

    val newMetadata = {
      metadata.copy(
        crs = destCrs,
        layout = targetLayerLayout,
        extent = targetDataExtent,
        bounds = metadata.bounds.setSpatialBounds(
          KeyBounds(targetLayerLayout.mapTransform.extentToBounds(targetDataExtent)))
      )
    }

    val newLayout = newMetadata.layout
    val maptrans = newLayout.mapTransform

    val rrp = implicitly[RasterRegionReproject[V]]

    val stagedTiles: RDD[(K, (Raster[V], RasterExtent, Polygon))] =
      bufferedTiles
        .mapPartitions { partition =>
          partition.flatMap { case (key, BufferedTile(tile, gridBounds)) => {
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
            val destRegion = ProjectedExtent(innerExtent, crs).reprojectAsPolygon(destCrs, 0.05)

            maptrans.keysForGeometry(destRegion).map { newKey =>
              val destRE = RasterExtent(maptrans(newKey), newLayout.tileLayout.tileCols, newLayout.tileLayout.tileRows)
              (key.setComponent[SpatialKey](newKey), (Raster(tile, outerExtent), destRE, destRegion))
            }
          }}
        }

    def createCombiner(tup: (Raster[V], RasterExtent, Polygon)) = {
        val (raster, destRE, destRegion) = tup
        rrp.regionReproject(raster, crs, destCrs, destRE, destRegion, rasterReprojectOptions.method, rasterReprojectOptions.errorThreshold).tile
      }

    def mergeValues(reprojectedTile: V, toReproject: (Raster[V], RasterExtent, Polygon)) = {
      val (raster, destRE, destRegion) = toReproject
      val destRaster = Raster(reprojectedTile, destRE.extent)
      // RDD.combineByKey contract allows us to safely re-use and mutate the accumulator, reprojectedTile
      rrp.regionReprojectMutable(raster, crs, destCrs, destRaster, destRegion, rasterReprojectOptions.method, rasterReprojectOptions.errorThreshold).tile
    }

    def mergeCombiners(reproj1: V, reproj2: V) = reproj1.merge(reproj2)

    val tiled: RDD[(K, V)] =
      targetPartitioner match {
        case Some(part) => stagedTiles.combineByKey(createCombiner, mergeValues, mergeCombiners, partitioner = part)
        case None => stagedTiles.combineByKey(createCombiner, mergeValues, mergeCombiners)
      }

    (targetZoom, ContextRDD(tiled, newMetadata))
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
    V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    destCrs: CRS,
    targetLayout: Either[LayoutScheme, LayoutDefinition],
    options: Options,
    partitioner: Option[Partitioner]
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
        apply(rdd, destCrs, targetLayout, bufferSize = 0, options = options, partitioner = partitioner)
      }
    } else {
      val crs = rdd.metadata.crs
      val layout = rdd.metadata.layout
      val tileLayout = rdd.metadata.layout.tileLayout
      // Avoid capturing instantiated Transform in closure because its not Serializable
      lazy val transform = Transform(crs, destCrs)

      val bufferedTiles =
        BufferTilesRDD(
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

      apply(bufferedTiles, rdd.metadata, destCrs, targetLayout, options, partitioner = partitioner)
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
    V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    destCrs: CRS,
    targetLayout: Either[LayoutScheme, LayoutDefinition],
    bufferSize: Int,
    options: Options,
    partitioner: Option[Partitioner]
  ): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    if(bufferSize == 0) {
      val fakeBuffers: RDD[(K, BufferedTile[V])] = rdd.withContext(_.mapValues { tile: V => BufferedTile(tile, GridBounds(0, 0, tile.cols - 1, tile.rows - 1)) })
      apply(fakeBuffers, rdd.metadata, destCrs, targetLayout, options, partitioner)
    } else
      apply(rdd.bufferTiles(bufferSize), rdd.metadata, destCrs, targetLayout, options, partitioner)


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
    val bounds: GridBounds[Int] = keyBounds match {
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

