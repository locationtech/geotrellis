package geotrellis.admin.services

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.{GET, Path}
import javax.ws.rs.core.{Response,Context}

@Path("/admin")
class Ping {
  @GET
  def get(@Context req:HttpServletRequest):Response = {
    val message = "pong"
    Response.ok(message)
            .`type`("text/plain")
            .build()
  }
}
