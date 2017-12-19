package geotrellis.spark.io.cog

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.util._
import geotrellis.vector._
import geotrellis.vector.io._

import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd._
import java.net.URI

import geotrellis.proj4.CRS

import scala.collection.JavaConverters._
import scala.reflect._

case class COGLayer[K, T <: CellGrid](
  layers: Map[ZoomRange, RDD[(K, GeoTiff[T])]], // Construct lower zoom levels off of higher zoom levels
  metadata: COGLayerMetadata[K]
)

object COGLayer {
  private def isPowerOfTwo(x: Int): Boolean =
    x != 0 && ((x & (x - 1)) == 0)

  // TODO: Remove
  case class ContextGeoTiff[K, T <: CellGrid](
    geoTiff: GeoTiff[T],
    metadata: TileLayerMetadata[K],
    zoom: Int,
    layoutScheme: ZoomedLayoutScheme,
    zoomRanges: Option[(Int, Int)],
    overviews: List[(Int, TileLayerMetadata[K])]
  )

  def apply[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    baseZoom: Int,
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
            List(range -> generateGeoTiffRDD(rdd, range, layoutScheme))
          } else {
            val previousLayer = acc.head._2.mapValues(_.overviews.last.tile)

            val rzz = generateGeoTiffRDD(previousLayer, range, layoutScheme)

            (range -> rzz) :: acc
          }
        }.
        toMap

    COGLayer(layers, cogLayerMetadata)
  }

  private def generateGeoTiffRDD[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](
    rdd: RDD[(K, V)],
    zoomRange: ZoomRange,
    layoutScheme: ZoomedLayoutScheme
  )(implicit tc: Iterable[(SpatialKey, V)] => GeoTiffSegmentConstructMethods[SpatialKey, V]): RDD[(K, GeoTiff[V])] = {
    val (minZoomLayout, maxZoomLayout) =
      (layoutScheme.levelForZoom(zoomRange.minZoom).layout, layoutScheme.levelForZoom(zoomRange.maxZoom).layout)

    val options: GeoTiffOptions =
      GeoTiffOptions(
        storageMethod = Tiled(maxZoomLayout.tileCols, maxZoomLayout.tileRows),
        compression = Deflate
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
        partition.map { case (key, tiles) =>
          val extent = key.getComponent[SpatialKey].extent(minZoomLayout)
          (key, createCog(tiles, zoomRange, layoutScheme, extent, layoutScheme.crs, options))
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
    options: GeoTiffOptions
  )(implicit tc: Iterable[(SpatialKey, V)] => GeoTiffSegmentConstructMethods[SpatialKey, V]): GeoTiff[V] = {
    val spatialTiles = tiles.map { case (key, value) => (key.getComponent[SpatialKey], value) }
    val accSeed = (List[GeoTiff[V]](), spatialTiles)
    val (overviews, _) =
      ((zoomRange.maxZoom - 1) to zoomRange.minZoom by -1).foldLeft(accSeed) { case ((acc, t), z) =>
        val prevLayout = layoutScheme.levelForZoom(z + 1).layout
        val thisLayout = layoutScheme.levelForZoom(z).layout
        val newTiles =
          t.
            groupBy { case (SpatialKey(c, r), tile) => SpatialKey(c/2, r/2) }.
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
            options
          )


        (gt :: acc, newTiles)
      }

    // {
    //   val layout = layoutScheme.levelForZoom(zoomRange.maxZoom).layout
    //   val gridBounds =
    //     layout.mapTransform.extentToBounds(extent)

    //   val tileLayout =
    //     TileLayout(gridBounds.width, gridBounds.height, layout.tileCols, layout.tileRows)

    //   sys.error(s"BOOM - ${tileLayout} ${zoomRange} ${spatialTiles.map(_._1).toSet.toSeq} ${extent.reproject(crs, LatLng).toPolygon.toGeoJson}")
    // }

    spatialTiles.toGeoTiff(
      layoutScheme.levelForZoom(zoomRange.maxZoom).layout,
      extent,
      crs,
      options,
      overviews = overviews
    )
  }

  def write[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](cogs: RDD[(K, GeoTiff[V])])(keyIndex: KeyIndex[K], uri: URI): Unit = {
    val conf = HadoopConfiguration(cogs.sparkContext.hadoopConfiguration)
    cogs.foreach { case (key, tiff) =>
      println(s"$key: ${uri.toString}/${keyIndex.toIndex(key)}.tiff")
      HdfsUtils.write(new Path(s"${uri.toString}/${keyIndex.toIndex(key)}.tiff"), conf.get) { new GeoTiffWriter(tiff, _).write(true) }
    }
  }
}
