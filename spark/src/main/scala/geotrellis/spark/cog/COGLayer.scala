package geotrellis.spark.cog

import java.net.URI

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.spark.io.hadoop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.stitch.Stitcher
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.pyramid.Pyramid.{Options => PyramidOptions}
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.stitch.TileLayoutStitcher
import geotrellis.spark.util._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector.Extent
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd._
import spire.syntax.cfor._

import scala.reflect.ClassTag

object COGLayer {
  /**
    * Effort to build TIFF without knowing anything about the rest of the segments
    * Probably can be easier done with the stitch function
    * @param rdd
    * @param startZoom
    * @param endZoom
    * @param layoutScheme
    * @tparam K
    * @tparam V
    * @return
    */

  def byBytes[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]//: ? => TiffBuilder[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme): SinglebandGeoTiff = {

    val levels: Stream[(Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]])] =
      Pyramid.levelStream[K, V, TileLayerMetadata[K]](rdd, layoutScheme, startZoom, endZoom, PyramidOptions.DEFAULT)

    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled)

    val res = levels.map { case (z, r) =>
      println(s"zoom: $z")

      val md: TileLayerMetadata[K] = r.metadata
      val crs = md.crs
      val extent = md.extent
      val te: TileBounds = md.bounds.asInstanceOf[KeyBounds[SpatialKey]].toGridBounds()

      val bandType = BandType.forCellType(md.cellType)
      val layoutCols = md.layoutCols
      val layoutRows = md.layoutRows
      val totalCols = te.width * md.tileLayout.tileCols // md.tileLayout.tileCols
      val totalRows = te.height * md.tileLayout.tileRows // md.tileLayout.tileRows

      //rdd.map
      //rdd.mapValues

      println(s"md.bounds: ${md.bounds}")
      println(s"te: ${te}")
      println(s"te.height: ${te.height}")
      println(s"te.width: ${te.width}")

      //GeoTiffSegmentLayout(totalCols, totalRows, TileLayout, storageMethod, interleaveMethod)

      val segmentLayout = GeoTiffSegmentLayout(totalCols, totalRows, TileLayout(te.width, te.height, md.tileLayout.tileCols, md.tileLayout.tileRows), options.storageMethod, BandInterleave)

      println(s"segmentLayout: ${segmentLayout}")

      def getSegmentIndex(col: Int, row: Int): Int = {
        val layoutCol = col / segmentLayout.tileLayout.tileCols
        val layoutRow = row / segmentLayout.tileLayout.tileRows
        (layoutRow * segmentLayout.tileLayout.layoutCols) + layoutCol
      }

      val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows

      println(s"segemntCount: ${segmentCount}")

      val compressor = options.compression.createCompressor(segmentCount)

      //val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)

      /*val segmentBytes: Array[Array[Byte]] =
        r
          .map { case (key, tile) =>
            //println(s"key: ${key}")

            // make it more generic think about colletion API too
            // and mb to use with anything
            val spatialKey = key.asInstanceOf[SpatialKey]
            val updateCol = (spatialKey.col - te.colMin) * md.tileLayout.tileCols
            val updateRow = (spatialKey.row - te.rowMin) * md.tileLayout.tileRows

            println(s"(updateCol, updateRow): ${(updateCol, updateRow)}")

            val bytes = tile.asInstanceOf[Tile].toBytes
            val i = getSegmentIndex(updateCol, updateRow)
            //val k = getSegmentIndex(spatialKey.col, spatialKey.row)
            //println(s"i: $i")
            //println(s"k: $k")

            i -> compressor.compress(bytes, i.toInt)
          }
          .map(_._2)
          .collect()*/

      val segmentBytes2 =
        r
          .map { case (key, tile) =>
            //println(s"key: ${key}")

            // make it more generic think about colletion API too
            // and mb to use with anything
            val spatialKey = key.asInstanceOf[SpatialKey]
            val updateCol = (spatialKey.col - te.colMin) * md.tileLayout.tileCols
            val updateRow = (spatialKey.row - te.rowMin) * md.tileLayout.tileRows

            println(s"(updateCol, updateRow): ${(updateCol, updateRow)}")

            val bytes = tile.asInstanceOf[Tile].toBytes
            val i = getSegmentIndex(updateCol, updateRow)
            val k = getSegmentIndex(spatialKey.col, spatialKey.row)
            println(s"i: $i")
            println(s"k: $k")

            i -> compressor.compress(bytes, i.toInt)
          }.cache()

      val sorted =
        segmentBytes2.map(_._1).collect().sorted

      val head = sorted.head
      val last = sorted.last

      val collection = segmentBytes2.collect().toMap
      val res: Array[Array[Byte]] =
        (head to last).map { i => collection.getOrElse(i, compressor.compress(Array[Byte](), i)) }.toArray

      /*val segmentTiles: Array[Tile] = r.map(_._2.asInstanceOf[Tile]).collect()

      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val bytes = segmentTiles(i).toBytes
        segmentBytes(i) = compressor.compress(bytes, i)
      }*/

      println(s"here")

      z -> (
        (new ArraySegmentBytes(res), compressor.createDecompressor, segmentLayout, options.compression, md.cellType),
        crs, extent
      )
    }

    val sortedRes = res.sortBy(- _._1).map(_._2)
    val baseLayer = sortedRes.headOption
    /*val overviews: List[SinglebandGeoTiff] = sortedRes.tail.map { case ((segments, decompressor, segmentLayout, compression, cellType), crs, extent) =>
      val geoTiffTile = GeoTiffTile(segments, decompressor, segmentLayout, compression, cellType)

      SinglebandGeoTiff(
       geoTiffTile,
        extent,
        crs,
        Tags.empty,
        options,
        Nil
      )
    }.toList*/

    val result = baseLayer.map { case ((segments, decompressor, segmentLayout, compression, cellType), crs, extent) =>
      val geoTiffTile = GeoTiffTile(segments, decompressor, segmentLayout, compression, cellType)

      SinglebandGeoTiff(
        geoTiffTile,
        extent,
        crs,
        Tags.empty,
        options.copy(subfileType = Some(ReducedImage)),
        Nil//overviews
      )
    }.get

    println(s"${result.cols -> result.rows}")
    result.overviews.map { r =>
      println(s"${r.cols -> r.rows}")
    }

    result
  }

  def pyramidUp[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](itr: Iterable[(K, V)],
    endZoom: Int,
    layoutLevel: LayoutLevel,
    layoutScheme: LayoutScheme,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions
   ): List[GeoTiff[V]] = {
    val nextLayoutLevel @ LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(layoutLevel)
    if(nextZoom >= endZoom) {
      val list: List[(K, V)] =
        itr
          .map { case (key, tile) =>
            val extent: Extent = key.getComponent[SpatialKey].extent(layoutLevel.layout)
            val newSpatialKey = nextLayout.mapTransform(extent.center)
            (key.setComponent(newSpatialKey), (key, tile))
          }
          .groupBy(_._1)
          .map { case (newKey, (nseq: Seq[(K, (K, V))])) =>
            val seq = nseq.map(_._2)
            val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
            val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

            for ((oldKey, tile) <- seq) {
              val oldExtent = oldKey.getComponent[SpatialKey].extent(layoutLevel.layout)
              newTile.merge(newExtent, oldExtent, tile, NearestNeighbor)
            }
            (newKey, newTile: V)
          }.toList

      val (stitchedTile: V, gb) =
        TileLayoutStitcher
          .stitch[V](list.map { case (k, v) => k.getComponent[SpatialKey] -> v })

      val ifdLayer: GeoTiff[V] =
        stitchedTile.toGeoTiff(nextLayout, gb, md, options.copy(subfileType = Some(ReducedImage)))

      ifdLayer :: pyramidUp(list, endZoom, nextLayoutLevel, layoutScheme, md, options)
    } else List()
  }

  def withStitch[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ? => GeoTiffConstructMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme): RDD[(K, GeoTiff[V])] = {
    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled)

    val md = rdd.metadata
    val sourceLayout = md.layout
    val LayoutLevel(_, endLayout) = layoutScheme.zoomOut(LayoutLevel(endZoom, sourceLayout))

    val groupedByEndZoom =
      rdd
        .map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val endSpatialKey = endLayout.mapTransform(extent.center)
          (key.setComponent(endSpatialKey), (key, tile))
        }
        .groupByKey()
        .cache()

    val groupedPartitions = groupedByEndZoom.count().toInt

    groupedByEndZoom
      .repartition(groupedPartitions)
      .mapPartitions { partition: Iterator[(K, (Iterable[(K, V)]))] =>
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        if(list.nonEmpty) {
          val sfc = list.head._1

          val (stitchedTile, gb) =
            TileLayoutStitcher
              .stitch[V](flatList.map { case (k, v) => k.getComponent[SpatialKey] -> v })

          val baseLayer: GeoTiff[V] =
            stitchedTile
              .toGeoTiff(
                sourceLayout,
                gb,
                md,
                options,
                pyramidUp[K, V](flatList, endZoom, LayoutLevel(startZoom, sourceLayout), layoutScheme, md, options)
              )

          Iterator(sfc -> baseLayer)
        } else Iterator()
      }
  }

  def write[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](cogs: RDD[(K, GeoTiff[V])])(keyIndex: KeyIndex[K], uri: URI): Unit = {
    val conf = HadoopConfiguration(cogs.sparkContext.hadoopConfiguration)
    cogs.foreach { case (key, tiff) =>
      HdfsUtils.write(new Path(s"${uri.toString}/${keyIndex.toIndex(key)}.tiff"), conf.get) { new GeoTiffWriter(tiff, _).write(true) }
    }
  }
}
