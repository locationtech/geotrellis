package geotrellis.admin

import geotrellis._
import geotrellis.rest.WebRunner
import geotrellis.process.{Server,Catalog}

// TODO: The GeoTrellis server shouldn't exist like this.
object Main {
  val server = Server("admin-server",
                      Catalog.fromPath(WebRunner.config.getString("geotrellis.catalog")))

  def run[T:Manifest](op: Op[T]) = server.getResult(op)
}
