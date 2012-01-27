package trellis.rest

import javax.ws.rs._

/**
 * Simple hello world rest service that responds to "/hello"
 */
@Path("/hello")
class HelloResource {
  @GET
  def hello() = "<h2>Hello Trellis!</h2>"
}
