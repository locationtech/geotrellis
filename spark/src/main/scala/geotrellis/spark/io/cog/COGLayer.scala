package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.io.geotiff.compression.Compression
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.util._
import geotrellis.vector._
import geotrellis.proj4.CRS
import geotrellis.spark.pyramid.Pyramid

import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd._
import spray.json._

import java.net.URI

import scala.reflect._

case class COGLayer[K, T <: CellGrid](
  layers: Map[ZoomRange, RDD[(K, GeoTiff[T])]], // Construct lower zoom levels off of higher zoom levels
  metadata: COGLayerMetadata[K]
 )

object COGLayer {
  private def isPowerOfTwo(x: Int): Boolean =
    x != 0 && ((x & (x - 1)) == 0)

  def apply[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](
     rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     baseZoom: Int,
     compression: Compression = Deflate,
     maxCOGTileSize: Int = 4096,
     minZoom: Option[Int] = None
   )(implicit tc: Iterable[(SpatialKey, V)] => GeoTiffSegmentConstructMethods[SpatialKey, V]): COGLayer[K, V] = {
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
        maxCOGTileSize
      )

    val layers: Map[ZoomRange, RDD[(K, GeoTiff[V])]] =
      cogLayerMetadata.zoomRanges.
        sorted(Ordering[ZoomRange].reverse).
        foldLeft(List[(ZoomRange, RDD[(K, GeoTiff[V])])]()) { case (acc, range) =>
          if(acc.isEmpty) {
            List(range -> generateGeoTiffRDD(rdd, range, layoutScheme, compression))
          } else {
            val previousLayer: RDD[(K, V)] = acc.head._2.mapValues { tiff =>
              if(tiff.overviews.nonEmpty) tiff.overviews.last.tile
              else tiff.tile
            }

            val tmd: TileLayerMetadata[K] = cogLayerMetadata.tileLayerMetadata(range.maxZoom + 1)
            val upsampledPreviousLayer =
              Pyramid.up(ContextRDD(previousLayer, tmd), layoutScheme, range.maxZoom + 1)._2

            val rzz = generateGeoTiffRDD(upsampledPreviousLayer, range, layoutScheme, compression)

            (range -> rzz) :: acc
          }
        }.
        toMap

    COGLayer(layers, cogLayerMetadata)
  }

  private def generateGeoTiffRDD[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](
     rdd: RDD[(K, V)],
     zoomRange: ZoomRange,
     layoutScheme: ZoomedLayoutScheme,
     compression: Compression
   )(implicit tc: Iterable[(SpatialKey, V)] => GeoTiffSegmentConstructMethods[SpatialKey, V]): RDD[(K, GeoTiff[V])] = {
    val kwFomat = KryoWrapper(implicitly[JsonFormat[K]])

    val (minZoomLayout, maxZoomLayout) = (
      layoutScheme.levelForZoom(zoomRange.minZoom).layout,
      layoutScheme.levelForZoom(zoomRange.maxZoom).layout
    )

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
          val extent = key.getComponent[SpatialKey].extent(minZoomLayout)
          (key, createCog(tiles, zoomRange, layoutScheme, extent, layoutScheme.crs, options, Tags(Map("GT_KEY" -> keyFormat.write(key).prettyPrint), Nil)))
        }
      }
  }

  private def createCog[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](
     tiles: Iterable[(K, V)],
     zoomRange: ZoomRange,
     layoutScheme: ZoomedLayoutScheme,
     extent: Extent,
     crs: CRS,
     options: GeoTiffOptions,
     tags: Tags
   )(implicit tc: Iterable[(SpatialKey, V)] => GeoTiffSegmentConstructMethods[SpatialKey, V]): GeoTiff[V] = {
    val spatialTiles = tiles.map { case (key, value) => (key.getComponent[SpatialKey], value) }

    val accSeed = (List[GeoTiff[V]](), spatialTiles)
    val (overviews, _) =
      ((zoomRange.maxZoom - 1) to zoomRange.minZoom by -1).foldLeft(accSeed) { case ((acc, t), z) =>
        val prevLayout = layoutScheme.levelForZoom(z + 1).layout
        val thisLayout = layoutScheme.levelForZoom(z).layout
        val newTiles =
          t.
            groupBy { case (k @ SpatialKey(c, r), _) =>
              SpatialKey(c / 2, r / 2)
            }.
            map { case (newKey, parts) =>
              // Make the prototype based on one of the constituent tiles, with same dimensions
              // (as always happens with power of two pyramids)
              val representative = parts.head._2
              val (cols, rows) = (representative.cols, representative.rows)
              val t = representative.prototype(cols, rows)
              val tExt = thisLayout.mapTransform.keyToExtent(newKey)
              val newTile =
                parts.foldLeft(t) { case (acc, (k, part)) =>
                  val partExt = prevLayout.mapTransform.keyToExtent(k)
                  acc.merge(tExt, partExt, part)
                }
              (newKey, newTile)
            }

        val gt: GeoTiff[V] =
          newTiles.toSeq.toGeoTiff(
            thisLayout,
            extent,
            crs,
            options.copy(subfileType = Some(ReducedImage))
          )


        (gt :: acc, newTiles)
      }

    spatialTiles.toGeoTiff(
      layoutScheme.levelForZoom(zoomRange.maxZoom).layout,
      extent,
      crs,
      options,
      overviews = overviews.reverse,
      tags = tags
    )
  }

  def write[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](cogs: RDD[(K, GeoTiff[V])])(keyIndex: KeyIndex[K], uri: URI): Unit = {
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
  def mergeCOGs[V <: CellGrid: ? => CropMethods[V]: ? => TileMergeMethods[V]: GeoTiffBuilder](
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
