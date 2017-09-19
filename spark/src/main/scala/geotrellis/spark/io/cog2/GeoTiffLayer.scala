package geotrellis.spark.io.cog2

import java.net.URI

import geotrellis.proj4.WebMercator
import geotrellis.raster.{CellGrid, Raster, RasterExtent, Tile}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffImageData, GeoTiffSegmentLayout}
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.SpatialKey
import geotrellis.spark.tiling.{LayoutDefinition, MapKeyTransform, ZoomedLayoutScheme}
import geotrellis.vector.Extent
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import geotrellis.spark.util.HadoopConfiguration
import geotrellis.util.StreamingByteReader
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

object GeoTiffLayerMetadata {
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

  def zoom(implicit sc: SparkContext) = layoutScheme.levelFor(metadata.extent, metadata.cellSize)
}

object GeoTiffLayer {
  def fromSinglebandFiles(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    tileDimensions: (Int, Int) = 256 -> 256
  )(implicit sc: SparkContext): GeoTiffLayer[Tile] =
    GeoTiffLayer(GeoTiffLayerMetadata.fetchSingleband(path, tileDimensions), layoutScheme)
}
