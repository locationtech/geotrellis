package geotrellis.spark.etl.hadoop

import java.math.BigInteger

import geotrellis.raster.Tile
import geotrellis.raster.render.ColorBreaks
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark._
import geotrellis.spark.render._
import org.apache.hadoop.conf.ConfServlet.BadFormatException
import org.apache.spark.rdd.RDD
import scala.reflect._


class SpatialRenderOutput extends OutputPlugin[SpatialKey, Tile, RasterMetaData] {
  def name = "render"
  def key = classTag[SpatialKey]
  def requiredKeys = Array("path", "format")
  def attributes(props: Map[String, String]) = null
  /**
   * Parses into ColorBreaks a string of limits and their colors in hex RGBA
   * Only used for rendering PNGs
   *
   * @param breaks ex: "23:cc00ccff;30:aa00aaff;120->ff0000ff"
   * @return
   */
  def parseBreaks(breaks: Option[String]): Option[ColorBreaks] = {
    breaks.map { blob =>
      try {
        val split = blob.split(";").map(_.trim.split(":"))
        val limits = split.map(pair => Integer.parseInt(pair(0)))
        val colors = split.map(pair => new BigInteger(pair(1), 16).intValue())
        ColorBreaks(limits, colors)
      } catch {
        case e: Exception =>
          throw new BadFormatException(s"Unable to parse breaks, expected '{limit}:{RGBA};{limit}:{RGBA};...' got: '$blob'")
      }
    }
  }
  
  override def apply(id: LayerId, rdd: RDD[(SpatialKey, Tile)] with Metadata[RasterMetaData], method: KeyIndexMethod[SpatialKey], props: Map[String, String]) =
    props("format").toLowerCase match {
      case "png" =>
        rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[RasterMetaData]].renderPng(id, props("path"), parseBreaks(props.get("breaks")))
      case "geotiff" =>
        rdd.asInstanceOf[RDD[(SpatialKey, Tile)] with Metadata[RasterMetaData]].renderGeoTiff(id, props("path"))
    }

  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) = ???
}

