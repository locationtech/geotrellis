package geotrellis.spark.io.cog3

import java.net.URI

import geotrellis.proj4.WebMercator
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.{Auto, AutoHigherResolution, Base, GeoTiff, GeoTiffImageData, GeoTiffSegmentLayout, GeoTiffSegmentLayoutTransform, GeoTiffTile, LazySegmentBytes, OverviewStrategy}
import geotrellis.raster.{CellGrid, CellSize, GridBounds, MutableArrayTile, Raster, RasterExtent, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog3.GeoTiffLayerMetadata.IndexRDD
import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod, ZCurveKeyIndexMethod}
import geotrellis.spark.tiling.{LayoutDefinition, MapKeyTransform, ZoomedLayoutScheme}
import geotrellis.spark.util._
import geotrellis.spark.{KeyBounds, LayerId, SpatialKey}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.Extent
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

case class GeoTiffMetadata[T <: CellGrid](path: URI, tiff: GeoTiff[T]) {
  def tiffTags(implicit sc: SparkContext) =
    TiffTags(StreamingByteReader(HdfsRangeReader(new Path(path), sc.hadoopConfiguration)))

  def imageData: GeoTiffImageData = tiff.imageData
  def segmentLayout: GeoTiffSegmentLayout = imageData.segmentLayout

  def segmentCount(implicit sc: SparkContext) = tiffTags.segmentCount
  def segmentByteCounts(implicit sc: SparkContext) = tiffTags.segmentByteCounts
  def segmentOffsets(implicit sc: SparkContext) = tiffTags.segmentOffsets

  def localMapTransform =
    MapKeyTransform(tiff.extent, imageData.segmentLayout.tileLayout.layoutDimensions)

  def crop(x: Int, y: Int, z: Int)(layoutScheme: ZoomedLayoutScheme): Raster[T] = {
    val layout = layoutScheme.levelForZoom(z).layout
    tiff.crop(layout.mapTransform(SpatialKey(x, y)), layout.cellSize)
  }

  // keys relative to the current tiff
  def localKeys: Iterator[SpatialKey] =
    localMapTransform(tiff.extent)
      .coordsIter
      .map { spatialComponent => spatialComponent: SpatialKey }

  // to persist them as Indexes?
  def keys(zoom: Int, layoutScheme: ZoomedLayoutScheme): Iterator[SpatialKey] =
    layoutScheme.levelForZoom(zoom).layout
      .mapTransform(tiff.extent)
      .coordsIter
      .map { spatialComponent => spatialComponent: SpatialKey }
}

case class GeoTiffLayerMetadata[T <: CellGrid: ClassTag](
  tiffs: RDD[GeoTiffMetadata[T]],
  tileDimensions: (Int, Int)
) {
  def extent: Extent = tiffs.map(_.tiff.extent).reduce(_ combine _)
  def cellSize(implicit sc: SparkContext) = tiffs.take(0).head.tiffTags.cellSize
  def rasterExtent(implicit sc: SparkContext) = RasterExtent(extent, tiffs.take(0).head.tiffTags.cellSize)
  def layout(implicit sc: SparkContext) = LayoutDefinition(rasterExtent, tileDimensions._1, tileDimensions._2)
  def mapTransform(implicit sc: SparkContext) = layout.mapTransform
  def tileLayout(implicit sc: SparkContext) = layout.tileLayout
  def layoutExtent(implicit sc: SparkContext) = layout.extent
  def gridBounds(implicit sc: SparkContext) = mapTransform(sc)(extent)

  def combine(other: GeoTiffLayerMetadata[T]): GeoTiffLayerMetadata[T] =
    this.copy(tiffs = tiffs union other.tiffs)
}

object IndexMethods {
  case class PersistentKeyIndexRDD[T <: CellGrid](
    ip: (KeyIndex[SpatialKey], List[(Int, IndexRDD)]),
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator)
  ) {
    val keyIndex: KeyIndex[SpatialKey] = ip._1
    val pyramid: Map[Int, IndexRDD] = ip._2.toMap

    def read(x: Int, y: Int, z: Int, name: Option[String] = None)(implicit sc: SparkContext): RDD[Raster[T]] = {
      val sconf = HadoopConfiguration(sc.hadoopConfiguration)
      val rdd: IndexRDD = pyramid(z)
      val layout = layoutScheme.levelForZoom(z).layout

      val zz = rdd
        .filter { case (lid, idx, (path, ranges)) =>
          val cn = path.toString.split("/").last.split("\\.").head

          println(s"lid.zoom == z && idx == keyIndex.toIndex(SpatialKey(x, y)) && name.map(n => n == cn).getOrElse(true): ${lid.zoom == z} && $idx == ${keyIndex.toIndex(SpatialKey(x, y))} && ${name.map(n => n == cn).getOrElse(true)}")
          lid.zoom == z && idx == keyIndex.toIndex(SpatialKey(x, y)) && name.map(n => n == cn).getOrElse(true)
        }
        .map { case (lid, idx, (path, ranges)) =>
          path -> ranges
        }
        .flatMap { case (path, ranges) =>
          val p = new Path(path)
          val tt = TiffTags(StreamingByteReader(HdfsRangeReader(p, sconf.get)))
          val segmentReader = LazySegmentBytes(StreamingByteReader(HdfsRangeReader(p, sconf.get)), tt)
          val geoTiffInfo = GeoTiffReader.readGeoTiffInfo(StreamingByteReader(HdfsRangeReader(p, sconf.get)), false, true)
          val geoTiffSingleband = GeoTiffReader.geoTiffSinglebandTile(geoTiffInfo)
          val segmentTransform =
            GeoTiffSegmentLayoutTransform(geoTiffInfo.segmentLayout, geoTiffInfo.bandCount)

          val res: Array[Raster[Tile]] =
            ranges.map { case (idx, _) =>
              Raster(geoTiffSingleband.crop(segmentTransform.getGridBounds(idx)), layout.mapTransform(SpatialKey(x, y)))
            }

          /*segmentTransform.getGridBounds()

          geoTiffSingleband.getSegment(1)

          //val tiff = GeoTiffReader.readSingleband(StreamingByteReader(HdfsRangeReader(p, sconf.get)), false, true)

          tiff.crop(tiff)

          tt.rasterExtent

            val bytes =
              ranges.map { case (idx, (offset, length)) =>
                segmentReader.getBytes(offset, length)
              }*/

          res.map(_.asInstanceOf[Raster[T]])
        }

      // a real pain here, makes sense to persist somehow indexes?
      // to put into our own tags / store in a separate file
      // would be not very cloud optimized though
      /*metadata.tiffs.collect { case md if md.keys(z, layoutScheme).contains(SpatialKey(x, y)) =>
        md.crop(x, y, z)(layoutScheme)
      }*/

      zz
    }

  }
}

object GeoTiffLayerMetadata {
  // LayerId, Index, (URI, Array[(SegmentId, (Ranges))])
  type Index = (LayerId, Long, (URI, Array[(Int, (Long, Long))]))
  type IndexRDD = RDD[Index]

  def fetchSingleband(path: URI, tileDimensions: (Int, Int) = 256 -> 256)(implicit sc: SparkContext): GeoTiffLayerMetadata[Tile] = {
    val sconf = HadoopConfiguration(sc.hadoopConfiguration)
    GeoTiffLayerMetadata(
      sc.parallelize(HdfsUtils.listFiles(new Path(path), sconf.get)).map { p =>
        val tiff = GeoTiffReader.readSingleband(StreamingByteReader(HdfsRangeReader(p, sconf.get)), false, true)
        GeoTiffMetadata(path, tiff)
      },
      tileDimensions
    )
  }

  def indexSingleband(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    keyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod,
    maxZoom: Option[Int] = None
  )(implicit sc: SparkContext): (KeyIndex[SpatialKey], List[(Int, IndexRDD)]) = {
    val sconf = HadoopConfiguration(sc.hadoopConfiguration)
    // pick a name from the tiff path
    //val name = "somename"

    // Map[Index -> (Path, Ranges)]
    val rdd: RDD[(URI, TiffTags)] = sc.parallelize(HdfsUtils.listFiles(new Path(path), sconf.get)).map { p =>
      p.toUri -> TiffTags(StreamingByteReader(HdfsRangeReader(p, sconf.get)))
    }

    val mz: Int = maxZoom match {
      case Some(v) => v
      case _ => {
        val tt = rdd.first._2
        layoutScheme.levelFor(tt.extent, tt.cellSize).zoom
      }
    }

    def keys(extent: Extent, zoom: Int): Iterator[SpatialKey] =
      layoutScheme.levelForZoom(zoom).layout
        .mapTransform(extent)
        .coordsIter
        .map { spatialComponent => spatialComponent: SpatialKey }

    // the entire key index space definition
    // in out terms on the "max" zoom level
    val collectKeys: KeyBounds[SpatialKey] = rdd.map { case (p, tt) =>
      val info: GeoTiffReader.GeoTiffInfo = GeoTiffReader.readGeoTiffInfo(StreamingByteReader(HdfsRangeReader(new Path(p), sconf.get)), false, true)
      // tt.extent // what to store / use on the building index step
      val ks: List[SpatialKey] = keys(info.extent, mz).toList
      KeyBounds(ks.min, ks.max)
    }.reduce(_ combine _)


    // store it to decode long => spatial key
    val keyIndex: KeyIndex[SpatialKey] = keyIndexMethod.createIndex(collectKeys)

    def encodeKey: SpatialKey => Long = k => keyIndex.toIndex(k)

    def buildPyramid(zoom: Int, rdd: RDD[(URI, TiffTags)]): List[(Int, IndexRDD)] = {
      val nrdd: IndexRDD =
        rdd.flatMap { case (p, tt) =>
          val name = p.toString.split("/").last.split("\\.").head
          val layout = layoutScheme.levelForZoom(zoom).layout
          val cellSize = layout.cellSize
          val mtt = layout.mapTransform
          println(s"MapKeyTransform(${tt.extent}, ${mtt.layoutCols}, ${mtt.layoutRows})")
          val mt: MapKeyTransform = MapKeyTransform(tt.extent, mtt.layoutCols, mtt.layoutRows)
          val info: GeoTiffReader.GeoTiffInfo = GeoTiffReader.readGeoTiffInfo(StreamingByteReader(HdfsRangeReader(new Path(p), sconf.get)), false, true)
          val geoTiffTile: GeoTiffTile = GeoTiffReader.geoTiffSinglebandTile(info)
          val overviews = geoTiffTile.overviews // probably smth is more best matching by resolution

          def getClosestOverview(cellSize: CellSize, strategy: OverviewStrategy): (Int, GeoTiffTile) = {
            overviews match {
              case Nil => -1 -> geoTiffTile
              case list =>
                strategy match {
                  case AutoHigherResolution =>
                    list
                      .zipWithIndex
                      .map { case (v, i) =>
                        i -> (tt.rasterExtent.copy(cols = v.cols, rows = v.rows).cellSize.resolution - cellSize.resolution, v)
                      }
                      .filter(_._2._1 >= 0)
                      .sortBy(_._2._1)
                      .map { case (i, (_, v)) => i -> v }
                      .headOption
                      .getOrElse(-1 -> geoTiffTile)
                  case Auto(n) =>
                    list
                      .sortBy { v =>
                        math.abs(tt.rasterExtent.copy(cols = v.cols, rows = v.rows).cellSize.resolution - cellSize.resolution)
                      }
                      .lift(n).map { v => n -> v }
                      .getOrElse(-1 -> geoTiffTile)
                  case Base => -1 -> geoTiffTile
                }
            }
          }

          val (ovrIdx, currentTiff) = getClosestOverview(cellSize, AutoHigherResolution)
          val currentInfo = if (ovrIdx >= 0) info.overviews(ovrIdx) else info

          val ks: List[SpatialKey] = keys(info.extent, zoom).toList //.sortBy { key => key.col -> key.row }
          val keyBounds: KeyBounds[SpatialKey] = KeyBounds(ks.min, ks.max)

          /*val ggbz: GridBounds =
            ks
              .map(key => tt.rasterExtent.gridBoundsFor(mt(key), true))
              .reduce(_ combine _)

          val ext = ks.map(mt(_)).reduce(_ combine _)
          val ggb: TileBounds = mt(ext)*/

          //println(s"keys(info.extent, $zoom).toList: ${keys(info.extent, zoom).toList}")

          //val gridBounds: GridBounds = keyBounds.toGridBounds()
          //val zz: Extent = mt(gridBounds)
          //val zz: Extent = mt(keyBounds)
          //val gb = mt(zz)


          /*val mt2 = MapKeyTransform(info.extent, tt.cols, tt.rows)
          println(s"mt.layoutCols: ${mt.layoutCols}")
          println(s"mt.layoutRows: ${mt.layoutRows}")
          println(s"tt.segmentCount: ${tt.segmentCount}")

          val gb = mt(mt(ks.min) combine mt(ks.max))
          println(s"gb: $gb")
          println(s"ggb: $ggb")
          println(s"ext: $ext")
          println(s"ext: ${info.extent}")
          println(s"ggbz: ${ggbz}")

          val gb2 =
            RasterExtent(info.extent, tt.cellSize)
              .gridBoundsFor(info.extent, clamp = true)*/

          println(s"tt.segmentCount: ${tt.segmentCount}")

          val currentTt = if (ovrIdx >= 0) tt.overviews(ovrIdx) else tt

          //val lsb = LazySegmentBytes(StreamingByteReader(HdfsRangeReader(p, sconf.get)), tt)

          val segmentOffsets = currentTt.segmentOffsets
          val segmentByteCounts = currentTt.segmentByteCounts

          // min thing to read is one segment
          // this fact creates additional restrictions
          // on geotiff layer usage
          // consider retiling segmnts layouts
          // WEBCog should be the perfect model
          // WARN: selects the best matching overview by resolution
          val finalRes: Array[(LayerId, Long, (URI, Array[(Int, (Long, Long))]))] =
            ks.map { key =>
              val re = RasterExtent(info.extent, tt.cellSize)
              val extent: Extent =
                layoutScheme
                  .levelForZoom(zoom)
                  .layout
                  .mapTransform(key)

              val gridBounds = re.gridBoundsFor(extent, clamp = true)
              val segments = currentTiff.getIntersectingSegments(gridBounds)
              val ranges: Array[(Int, (Long, Long))] =
                segments.map { i =>
                  val startOffset = segmentOffsets(i)
                  val endOffset = segmentOffsets(i) + segmentByteCounts(i) - 1
                  (i, startOffset -> endOffset)
                }

              (LayerId(name, zoom), keyIndex.toIndex(key), (p, ranges))
            }.toArray

          // layerId keeps an array of idx -> (path, (segmentId, startOffset, endOffset))
          // val finalRes: (LayerId, (Path, Array[(Long, Array[(Int, (Long, Long))])])) =
          //  LayerId(name, zoom) -> (p, res)

          // the query would be
          // 1. filter by layerId // pick the correct name at zoom
          // 2. filter by ranges to pick
          // 3. the result would be offsets to read from tiff

          finalRes
        }

      if (zoom >= 1) {
        // rdd.persist(cacheLevel)
        //  sink(rdd, zoom)

        (zoom, nrdd) :: buildPyramid(zoom - 1, rdd)
      } else {
        //sink(rdd, zoom)
        List((zoom, nrdd))
      }
    }

    keyIndex -> buildPyramid(mz, rdd)
  }
}

case class GeoTiffLayer[T <: CellGrid](metadata: GeoTiffLayerMetadata[T], layoutScheme: ZoomedLayoutScheme) {
  // that should be very very slow
  def read(x: Int, y: Int, z: Int)(implicit sc: SparkContext): RDD[Raster[T]] = {
    // a real pain here, makes sense to persist somehow indexes?
    // to put into our own tags / store in a separate file
    // would be not very cloud optimized though
    metadata.tiffs.collect { case md if md.keys(z, layoutScheme).contains(SpatialKey(x, y)) =>
      md.crop(x, y, z)(layoutScheme)
    }
  }

  def read: RDD[Raster[T]] = metadata.tiffs.map { _.tiff.raster }
  def read(name: String): RDD[Raster[T]] =
    metadata
      .tiffs
      .collect { case md if md.path.toString.contains(name) => md.tiff.raster }

  def read(name: String, x: Int, y: Int, z: Int): RDD[Raster[T]] =
    metadata
      .tiffs
      .filter { md => md.path.toString.contains(name) && md.keys(z, layoutScheme).contains(SpatialKey(x, y)) }
      .map { md =>
        md.crop(x, y, z)(layoutScheme)
      }
}

object GeoTiffLayer {
  def fromSinglebandFiles(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    tileDimensions: (Int, Int) = 256 -> 256
  )(implicit sc: SparkContext): GeoTiffLayer[Tile] =
    GeoTiffLayer(GeoTiffLayerMetadata.fetchSingleband(path, tileDimensions), layoutScheme)
}
