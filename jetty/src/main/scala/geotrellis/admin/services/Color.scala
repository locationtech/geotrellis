package geotrellis.admin.services

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.admin._
import geotrellis.admin.Json._
import geotrellis.rest._

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
