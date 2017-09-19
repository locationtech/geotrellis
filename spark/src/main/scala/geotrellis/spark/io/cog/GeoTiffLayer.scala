package geotrellis.spark.io.cog

import geotrellis.proj4.WebMercator
import geotrellis.raster.{CellGrid, Raster, RasterExtent, Tile}
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.SpatialKey
import geotrellis.spark.io.cog.COGMetadataReader.md.GeoTiffMetadata
import geotrellis.spark.tiling.{LayoutDefinition, ZoomedLayoutScheme}
import geotrellis.vector.Extent
import java.nio.file.{Files, Paths}

import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import geotrellis.util.StreamingByteReader
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

case class GeoTiffLayerMetadata[T <: CellGrid](
  tileDimensions: (Int, Int) = 256 -> 256,
  tiffs: List[GeoTiffMetadata[T]] = Nil
) {
  def extent: Extent = tiffs.map(_.tiff.extent).reduceLeft(_ combine _)
  def rasterExtent = RasterExtent(extent, tiffs.head.tiffTags.cellSize)
  def layout = LayoutDefinition(rasterExtent, tileDimensions._1, tileDimensions._2)
  def mapTransform = layout.mapTransform
  def tileLayout = layout.tileLayout
  def layoutExtent = layout.extent
  def gridBounds = mapTransform(extent)

  def combine(other: GeoTiffLayerMetadata[T]): GeoTiffLayerMetadata[T] =
    this.copy(tiffs = tiffs ::: other.tiffs)
}

object GeoTiffLayerMetadata {
  def fetchSingleband(tileDimensions: (Int, Int) = 256 -> 256, tiffPaths: List[String] = "tiff" :: Nil): GeoTiffLayerMetadata[Tile] =
    GeoTiffLayerMetadata(
      tileDimensions,
      tiffs = tiffPaths.map { path =>
        GeoTiffMetadata(SinglebandGeoTiff(path, false, true), path)
      }
    )
}

case class GeoTiffLayer[T <: CellGrid](metadata: GeoTiffLayerMetadata[T], layoutScheme: ZoomedLayoutScheme) {
  // that should be very very slow
  def read(x: Int, y: Int, z: Int): List[Raster[T]] = {
    // a real pain here, makes sense to persist somehow indexes?
    // to put into our own tags / store in a separate file
    // would be not very cloud optimized though
    metadata.tiffs.collect { case md if md.keys(z, layoutScheme).contains(SpatialKey(x, y)) =>
      md.crop(x, y, z)(layoutScheme)
    }
  }

  def read: List[Raster[T]] = metadata.tiffs.map { _.tiff.raster }
  def read(name: String): List[Raster[T]] =
    metadata
      .tiffs
      .collect { case md if md.path.contains(name) => md.tiff.raster }

  def read(name: String, x: Int, y: Int, z: Int): List[Raster[T]] =
    metadata
      .tiffs
      .collect { case md if md.path.contains(name) && md.keys(z, layoutScheme).contains(SpatialKey(x, y)) =>
        md.crop(x, y, z)(layoutScheme)
      }

}

object GeoTiffLayer {
  import scala.collection.JavaConverters._
  // not a Spark Job for now
  def fromFiles(
    path: String,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator)
  )/*(implicit sc: SparkContext)*/: GeoTiffLayer[Tile] = {
    /*val conf = sc.hadoopConfiguration
    HdfsUtils.listFiles(path, conf).map { p =>
      val byteReader = StreamingByteReader(HdfsRangeReader(p, conf))

    }*/


    //val byteReader = StreamingByteReader(HdfsRangeReader(path, conf))

    val tiffPaths =
      Files
        .walk(Paths.get(path))
        .iterator()
        .asScala
        .toList
        .collect { case p if Files.isRegularFile(p) => p.toAbsolutePath.toString }

    // let it be smth persistable but in mem for now
    GeoTiffLayer(GeoTiffLayerMetadata.fetchSingleband(tiffPaths = tiffPaths), layoutScheme)
  }

  // path to the index file?
  def apply(path: String) = {

  }
}
