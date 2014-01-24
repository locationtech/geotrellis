package geotrellis.jetty.service

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context}

import geotrellis._
import geotrellis.jetty._
import geotrellis.services._

import scala.collection.JavaConversions._

@Path("/admin/catalog")
class CatalogService {
  @GET
  def catalog(
    @Context req:HttpServletRequest
  ):Response = {
    OK.json(CatalogService.getJson)
      .allowCORS()
  }
}
