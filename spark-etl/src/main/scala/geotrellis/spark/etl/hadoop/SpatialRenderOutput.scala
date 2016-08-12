package geotrellis.spark.etl.hadoop

import java.math.BigInteger

import geotrellis.raster.Tile
import geotrellis.raster.render._
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark._
import geotrellis.spark.etl.config.{Backend, EtlConf}
import geotrellis.spark.render._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3._
import org.apache.hadoop.conf.ConfServlet.BadFormatException

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect._

class SpatialRenderOutput extends OutputPlugin[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def name = "render"
  def key = classTag[SpatialKey]
  def attributes(conf: EtlConf) = null
  /**
   * Parses to a ColorMap a string of limits and their colors in hex RGBA
   * Only used for rendering PNGs
   *
   * @param color map in "BREAK:COLOR" format, ex: "23:cc00ccff;30:aa00aaff;120->ff0000ff"
   * @return ColorMap
   */
  def parseColorMaps(classifications: Option[String]): Option[ColorMap] = {
    classifications.map { blob =>
      try {
        val split = blob.split(";").map(_.trim.split(":"))
        val limits = split.map(pair => Integer.parseInt(pair(0)))
        val colors = split.map(pair => new BigInteger(pair(1), 16).intValue())
        ColorMap(limits, colors)
      } catch {
        case e: Exception =>
          throw new BadFormatException(s"Unable to parse classifications, expected '{limit}:{RGBA};{limit}:{RGBA};...' got: '$blob'")
      }
    }
  }

  override def apply(
    id: LayerId,
    rdd: RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]],
    conf: EtlConf
  ): Unit = {
    val useS3 = getPath(conf.output.backend).path.take(5) == "s3://"
    val images =
      conf.output.encoding.get.toLowerCase match {
          case "png" =>
            parseColorMaps(conf.output.breaks) match {
              case Some(colorMap) =>
                rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]].renderPng(colorMap).mapValues(_.bytes)
              case None =>
                rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]].renderPng().mapValues(_.bytes)
            }
          case "jpg" =>
            parseColorMaps(conf.output.breaks) match {
              case Some(colorMap) =>
                rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]].renderJpg(colorMap).mapValues(_.bytes)
              case None =>
                rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]].renderJpg().mapValues(_.bytes)
            }
          case "geotiff" =>
            rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]].renderGeoTiff().mapValues(_.toByteArray)
        }

    if (useS3) {
      val keyToPath = SaveToS3.spatialKeyToPath(id, getPath(conf.output.backend).path)
      images.saveToS3(keyToPath)
    }
    else {
      val keyToPath = SaveToHadoop.spatialKeyToPath(id, getPath(conf.output.backend).path)
      images.saveToHadoop(keyToPath)
    }
  }

  def writer(conf: EtlConf)(implicit sc: SparkContext) = ???
}
