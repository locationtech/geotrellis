package geotrellis.jetty.service

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.jetty._
import geotrellis.service._

@Path("/admin/colors")
class Color {
  @GET
  def get(
    @Context req:HttpServletRequest
  ):Response = {
    // Return JSON with information on color ramps.
    OK.json(ColorRampMap.getJson)
      .allowCORS()
  }
}
