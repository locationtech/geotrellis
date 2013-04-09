package geotrellis.admin.services

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.admin._
import geotrellis.admin.Json._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.rest._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.feature.op.geometry.AsPolygonSet
import geotrellis.feature.rasterize.{Rasterizer, Callback}
import geotrellis.data.ColorRamps._

import scala.collection.JavaConversions._

/**
 * Create a weighted overlay of the Chattanooga model.
 */
@Path("/admin/colors")
class Color {
  @GET
  def get(
    @Context req:HttpServletRequest
  ):Response = {
    // Return JSON with information on color ramps.
    val c = for(key <- Colors.rampMap.keys) yield {
      s"""{ "key": "$key", "image": "img/ramps/${key}.png" }"""
    }
    val arr = "[" + c.mkString(",") + "]"
    OK.json(s"""{ "colors": $arr }""")
      .allowCORS()
  }
}
