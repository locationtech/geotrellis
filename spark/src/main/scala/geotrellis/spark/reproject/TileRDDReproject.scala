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
import geotrellis.util.LazyLogging

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
    * @param            bufferedTiles                An RDD of buffered tiles, created using the BufferTiles operation.
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
    val mapTransform: MapKeyTransform = layout.mapTransform
    val tileLayout: TileLayout = layout.tileLayout

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
            val innerExtent = mapTransform(key)
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

    val (zoom, newMetadata, tilerResampleMethod) =
      targetLayout match {
        case Left(layoutScheme) =>
          // If it's a floating layout scheme, the cell grid will line up and we always want to use nearest neighbor resampling
          val (z, m) = reprojectedTiles.collectMetadata(destCrs, layoutScheme)
          layoutScheme match {
            case _: FloatingLayoutScheme =>
              (z, m, NearestNeighbor)
            case _ =>
              (z, m, options.rasterReprojectOptions.method)
          }
        case Right(layoutDefinition) =>
          val m = reprojectedTiles.collectMetadata(destCrs, layoutDefinition)
          (0, m, options.rasterReprojectOptions.method)
      }

    // Layout change may imply upsampling, creating more tiles. If so compensate by creating more partitions
    val part: Option[Partitioner] =
      for {
        sourceBounds <- metadata.bounds.toOption
        targetBounds <- newMetadata.bounds.toOption
        sizeRatio = targetBounds.toGridBounds.sizeLong.toDouble / sourceBounds.toGridBounds.sizeLong.toDouble
        if sizeRatio > 1.5
      } yield {
        val newPartitionCount = (bufferedTiles.partitions.length * sizeRatio).toInt
        logger.info(s"Layout change grows potential number of tiles by $sizeRatio times, resizing to $newPartitionCount partitions.")
        new HashPartitioner(partitions = newPartitionCount )
      }

    val tiled = reprojectedTiles.tileToLayout(newMetadata,
      Tiler.Options(resampleMethod = tilerResampleMethod,
        partitioner = if (part.isDefined) part else bufferedTiles.partitioner ))

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
      val mapTransform = rdd.metadata.layout.mapTransform
      val tileLayout = rdd.metadata.layout.tileLayout

      val rasterExtents: RDD[(K, (RasterExtent, RasterExtent))] =
        rdd
          .mapPartitions({ partition =>
            val transform = Transform(crs, destCrs)

            partition.map { case (key, _) =>
              val extent = mapTransform(key)
              val rasterExtent = RasterExtent(extent, tileLayout.tileCols, tileLayout.tileRows)
              (key, (rasterExtent, ReprojectRasterExtent(rasterExtent, transform)))
            }
          }, preservesPartitioning = true)

      val borderSizesPerKey =
        rasterExtents
          .mapValues { case (re1, re2) =>
            // Reproject the extent back into the original CRS,
            // to determine how many border pixels we need.
            // Pad by one extra pixel.
            val e = re2.extent.reproject(destCrs, crs)
            val gb = re1.gridBoundsFor(e, clamp = false)
            BufferSizes(
              left = 1 + (if(gb.colMin < 0) -gb.colMin else 0),
              right = 1 + (if(gb.colMax >= re1.cols) gb.colMax - (re1.cols - 1) else 0),
              top = 1 + (if(gb.rowMin < 0) -gb.rowMin else 0),
              bottom = 1 + (if(gb.rowMax >= re1.rows) gb.rowMax - (re1.rows - 1) else 0)
            )
          }

      val bufferedTiles =
        rdd.bufferTiles(borderSizesPerKey)

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
}
