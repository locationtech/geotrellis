package geotrellis.spark.etl.hadoop

import geotrellis.raster.render.ColorBreaks
import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpatialKey, RasterRDD, LayerId}
import geotrellis.spark.render._
import scala.reflect._


class SpatialRenderOutput extends OutputPlugin {
  def name = "render"
  def key = classTag[SpatialKey]
  def requiredKeys = Array("path", "format")
  def attributes(props: Map[String, String]) = sys.error("Render plugins do not have AttributesStore")

  /**
   * Parses into ColorBreaks a string of limits and their colors in hex RGBA
   * Only used for rendering PNGs
   *
   * @param breaks ex: "23:cc00ccff,30:aa00aaff,120:ff0000ff"
   * @return
   */
  def parseBreaks(breaks: Option[String]): Option[ColorBreaks] = {
    breaks.map { blob =>
      val split = blob.split(",").map(_.trim.split(":"))
      val limits = split.map( pair => Integer.parseInt(pair(0)))
      val colors = split.map( pair => Integer.parseInt(pair(1), 16))
      ColorBreaks(limits, colors)
    }
  }
  
  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) =
    props("format").toLowerCase match {
      case "png" =>
        rdd.asInstanceOf[RasterRDD[SpatialKey]].renderPng(id, props("path"), parseBreaks(props.get("breaks")))
      case "geotiff" =>
        rdd.asInstanceOf[RasterRDD[SpatialKey]].renderGeoTiff(id, props("path"))
    }

}

