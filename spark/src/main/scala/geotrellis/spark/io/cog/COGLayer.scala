/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io.cog

import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.io.geotiff.compression.Compression
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample.ResampleMethod
import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.layers.cog.{COGLayerMetadata, ZoomRange}
import geotrellis.layers.hadoop.{SerializableConfiguration, HdfsUtils}
import geotrellis.layers.index.KeyIndex

import geotrellis.spark._
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.util._

import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd._

import spray.json._

import scala.reflect._
import java.net.URI


case class COGLayer[K, T <: CellGrid[Int]](
  layers: Map[ZoomRange, RDD[(K, GeoTiff[T])]], // Construct lower zoom levels off of higher zoom levels
  metadata: COGLayerMetadata[K]
 )

object COGLayer {
  private def isPowerOfTwo(x: Int): Boolean =
    x != 0 && ((x & (x - 1)) == 0)

  /**
    * Builds [[COGLayer]] pyramid from a base layer.
    *
    * @param rdd             Layer layer, at highest resolution
    * @param baseZoom        Zoom level of the base layer, assumes [[ZoomedLayoutScheme]]
    * @param minZoom         Zoom level at which to stop the pyramiding.
    * @param options         [[COGLayerWriter.Options]] that contains information on the maxTileSize,
    *                        resampleMethod, and compression of the written layer.
    */
  def fromLayerRDD[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffBuilder
  ](
     rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     baseZoom: Int,
     minZoom: Option[Int] = None,
     options: COGLayerWriter.Options = COGLayerWriter.Options.DEFAULT
   ): COGLayer[K, V] = {
    // TODO: Clean up conditional checks, figure out how to bake into type system, or report errors better.

    if(minZoom.getOrElse(Double.NaN) != baseZoom.toDouble) {
      if(rdd.metadata.layout.tileCols != rdd.metadata.layout.tileRows) {
        sys.error("Cannot create Pyramided COG layer for non-square tiles.")
      }

      if(!isPowerOfTwo(rdd.metadata.layout.tileCols)) {
        sys.error("Cannot create Pyramided COG layer for tile sizes that are not power-of-two.")
      }
    }

    val layoutScheme =
      ZoomedLayoutScheme(rdd.metadata.crs, rdd.metadata.layout.tileCols)

    if(rdd.metadata.layout != layoutScheme.levelForZoom(baseZoom).layout) {
      sys.error(s"Tile Layout of layer and ZoomedLayoutScheme do not match. ${rdd.metadata.layout} != ${layoutScheme.levelForZoom(baseZoom).layout}")
    }

    val keyBounds =
      rdd.metadata.bounds match {
        case kb: KeyBounds[K] => kb
        case _ => sys.error(s"Cannot create COGLayer with empty Bounds")
      }

    val cogLayerMetadata: COGLayerMetadata[K] =
      COGLayerMetadata(
        rdd.metadata.cellType,
        rdd.metadata.extent,
        rdd.metadata.crs,
        keyBounds,
        layoutScheme,
        baseZoom,
        minZoom.getOrElse(0),
        options.maxTileSize
      )

    val compression = options.compression
    val resampleMethod = options.resampleMethod

    val pyramidOptions = Pyramid.Options(resampleMethod = resampleMethod)

    val layers: Map[ZoomRange, RDD[(K, GeoTiff[V])]] =
      cogLayerMetadata.zoomRanges.
        sorted(Ordering[ZoomRange].reverse).
        foldLeft(List[(ZoomRange, RDD[(K, GeoTiff[V])])]()) { case (acc, range) =>
          if(acc.isEmpty) {
            List(range -> generateGeoTiffRDD(rdd, range, layoutScheme, cogLayerMetadata.cellType, compression, resampleMethod))
          } else {
            val previousLayer: RDD[(K, V)] = acc.head._2.mapValues { tiff =>
              if(tiff.overviews.nonEmpty) tiff.overviews.last.tile
              else tiff.tile
            }

            val tmd: TileLayerMetadata[K] = cogLayerMetadata.tileLayerMetadata(range.maxZoom + 1)
            val upsampledPreviousLayer =
              Pyramid.up(ContextRDD(previousLayer, tmd), layoutScheme, range.maxZoom + 1, pyramidOptions)._2

            val rzz = generateGeoTiffRDD(upsampledPreviousLayer, range, layoutScheme, cogLayerMetadata.cellType, compression, resampleMethod)

            (range -> rzz) :: acc
          }
        }.
        toMap

    COGLayer(layers, cogLayerMetadata)
  }

  private def generateGeoTiffRDD[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffBuilder
  ](
     rdd: RDD[(K, V)],
     zoomRange: ZoomRange ,
     layoutScheme: ZoomedLayoutScheme,
     cellType: CellType,
     compression: Compression,
     resampleMethod: ResampleMethod
   ): RDD[(K, GeoTiff[V])] = {
    val kwFomat = KryoWrapper(implicitly[JsonFormat[K]])
    val crs = layoutScheme.crs

    val minZoomLayout = layoutScheme.levelForZoom(zoomRange.minZoom).layout
    val maxZoomLayout = layoutScheme.levelForZoom(zoomRange.maxZoom).layout

    val options: GeoTiffOptions =
      GeoTiffOptions(
        storageMethod = Tiled(maxZoomLayout.tileCols, maxZoomLayout.tileRows),
        compression = compression
      )

    rdd.
      mapPartitions { partition =>
        partition.map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(maxZoomLayout)
          val minZoomSpatialKey = minZoomLayout.mapTransform(extent.center)

          (key.setComponent(minZoomSpatialKey), (key, tile))
        }
      }.
      groupByKey(new HashPartitioner(rdd.partitions.length)).
      mapPartitions { partition =>
        val keyFormat = kwFomat.value
        partition.map { case (key, tiles) =>
          val cogExtent = key.getComponent[SpatialKey].extent(minZoomLayout)
          val centerToCenter: Extent = {
            val h = maxZoomLayout.cellheight / 2
            val w = maxZoomLayout.cellwidth / 2
            Extent(
              xmin = cogExtent.xmin + w,
              ymin = cogExtent.ymin + h,
              xmax = cogExtent.xmax - w,
              ymax = cogExtent.ymax - h)
          }
          val cogTileBounds: TileBounds = maxZoomLayout.mapTransform.extentToBounds(centerToCenter)
          val cogLayout: TileLayout = maxZoomLayout.layoutForBounds(cogTileBounds).tileLayout

          val segments = tiles.map { case (key, value) =>
            val SpatialKey(col, row) = key.getComponent[SpatialKey]
            (SpatialKey(col - cogTileBounds.colMin, row - cogTileBounds.rowMin), value)
          }

          val cogTile = GeoTiffBuilder[V].makeTile(
            segments.iterator,
            cogLayout,
            cellType,
            Tiled(cogLayout.tileCols, cogLayout.tileRows),
            compression)

          val cogTiff = GeoTiffBuilder[V].makeGeoTiff(
            cogTile, cogExtent, crs,
            Tags(Map("GT_KEY" -> keyFormat.write(key).prettyPrint), Nil),
            options
          ).withOverviews(resampleMethod)

          (key, cogTiff)
        }
      }
  }

  def write[K: SpatialComponent: ClassTag, V <: CellGrid[Int]: ClassTag](cogs: RDD[(K, GeoTiff[V])])(keyIndex: KeyIndex[K], uri: URI): Unit = {
    val conf = SerializableConfiguration(cogs.sparkContext.hadoopConfiguration)
    cogs.foreach { case (key, tiff) =>
      println(s"$key: ${uri.toString}/${keyIndex.toIndex(key)}.tiff")
      HdfsUtils.write(new Path(s"${uri.toString}/${keyIndex.toIndex(key)}.tiff"), conf.value) { new GeoTiffWriter(tiff, _).write(true) }
    }
  }

  /**
    * Merge two COGs, may be used in COG layer update.
    * Merge will happen on per-segment basis, avoiding decompressing all segments at once.
    */
  def mergeCOGs[V <: CellGrid[Int]: ? => CropMethods[V]: ? => TileMergeMethods[V]: GeoTiffBuilder](
    previous: GeoTiff[V],
    update: GeoTiff[V]
  ): GeoTiff[V] = {
    val geoTiffBuilder = implicitly[GeoTiffBuilder[V]]
    // require previous layout is the same as current layout
    val Tiled(segmentCols, segmentRows) = previous.options.storageMethod
    val pixelCols = previous.tile.cols
    val pixelRows = previous.tile.rows
    val layout = TileLayout(
      layoutRows = math.ceil(pixelRows.toDouble / segmentRows).toInt,
      layoutCols = math.ceil(pixelCols.toDouble / segmentCols).toInt,
      tileCols = segmentCols,
      tileRows = segmentRows)

    // TODO Can we use tile.crop(Seq[GridBounds]) here?
    // Can we rely on method dispatch to pickup GeoTiffTile implementation?
    val tiles: Seq[(SpatialKey, V)] = for {
      layoutRow <- 0 until layout.layoutRows
      layoutCol <- 0 until layout.layoutCols
      segmentBounds = GridBounds(
        colMin = layoutCol * segmentCols,
        rowMin = layoutRow * segmentRows,
        colMax = (layoutCol + 1) * segmentCols - 1,
        rowMax = (layoutRow + 1) * segmentRows - 1)
    } yield {
      /* Making the assumption here that the segments are going to be tiled matching the layout tile size.
       * Otherwise this access pattern, individual crops, may result in a lot of repeated IO.
       */
      val key = SpatialKey(layoutCol, layoutRow)
      val left = previous.tile.crop(segmentBounds)
      val right = update.tile.crop(segmentBounds)
      (key, left.merge(right))
    }

    // NEXT rebuild overviews
    geoTiffBuilder.fromSegments(
      tiles.toMap,
      LayoutDefinition(previous.extent, layout).mapTransform.keyToExtent,
      previous.crs,
      previous.options,
      previous.tags
    )
  }
}
