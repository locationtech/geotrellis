package geotrellis.admin.services

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.admin._
import geotrellis.admin.Json._
import geotrellis.rest._

import scala.collection.JavaConversions._

@Path("/admin/catalog")
class CatalogService {
  @GET
  def catalog(
    @Context req:HttpServletRequest
  ):Response = {
    OK.json(GeoTrellis.server.catalog.toJson)
      .allowCORS()
  }
}
