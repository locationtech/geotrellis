package geotrellis.spark.io.geotiff

import geotrellis.proj4.WebMercator
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.{Raster, Tile}
import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.util._
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.Extent

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.net.URI

case class SinglebandGeoTiffRDDLayerReader(
  rdd: RDD[(Extent, URI)],
  layoutScheme: ZoomedLayoutScheme,
  discriminator: URI => String
) extends GeoTiffRDDLayerReader[Tile] {
  def read(layerId: LayerId)(x: Int, y: Int)(implicit sc: SparkContext): Raster[Tile] = {
    val sconf = HadoopConfiguration(sc.hadoopConfiguration)

    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform

    rdd
      .filter { case (extent, p) =>
        extent.intersects(mapTransform(SpatialKey(x, y))) && layerId.name == discriminator(p)
      }
      .map { case (_, uri) =>
        GeoTiffReader
          .readSingleband(StreamingByteReader(HdfsRangeReader(new Path(uri), sconf.get)), false, true)
          .crop(mapTransform(SpatialKey(x, y)), layout.cellSize)
      }
      .reduce(_ merge _)
  }

  def readAll(layerId: LayerId)(implicit sc: SparkContext): RDD[Raster[Tile]] = {
    val sconf = HadoopConfiguration(sc.hadoopConfiguration)

    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    rdd
      .filter { case (_, p) => layerId.name == discriminator(p) }
      .map { case (extent, uri) =>
        GeoTiffReader
          .readSingleband(StreamingByteReader(HdfsRangeReader(new Path(uri), sconf.get)), false, true)
          .crop(extent, layout.cellSize)
      }
  }
}

object SinglebandGeoTiffRDDLayerReader {
  def fetchSingleband(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    discriminator: URI => String = uri => uri.toString.split("/").last.split("\\.").head
  )(implicit sc: SparkContext): GeoTiffRDDLayerReader[Tile] = {
    val sconf = HadoopConfiguration(sc.hadoopConfiguration)
    val rdd: RDD[(Extent, URI)] =
      sc.parallelize(HdfsUtils.listFiles(new Path(path), sconf.get))
        .map { p =>
          val tiff = GeoTiffReader.readSingleband(StreamingByteReader(HdfsRangeReader(p, sconf.get)), false, true)
          tiff.extent -> p.toUri
        }

    SinglebandGeoTiffRDDLayerReader(rdd, layoutScheme, discriminator)
  }
}
