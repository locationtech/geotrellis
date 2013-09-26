package geotrellis.rest

import geotrellis._
import geotrellis.process._

object GeoTrellis {
  private var _server:Server = null
  def server = {
    if(_server == null) { 
      sys.error("The GeoTrellis object has not been set up properly. " +
                "You must call GeoTrellis.setup before using the GeoTrellis server.")
    }
    _server
  }

  def setup(config:GeoTrellisConfig, name:String = "geotrellis-server") = {
    val catalog = config.catalogPath match {
      case Some(path) =>
        if(!new java.io.File(path).exists()) {
          sys.error(s"Catalog path $path does not exist. Please modify your settings.")
        }
        Catalog.fromPath(path)
      case None => Catalog.empty(s"${name}-catalog")
    }
    _server = Server(name, catalog)
  }

  def run[T:Manifest](op: Op[T]) = {
    server.getResult(op)
  }

  def shutdown() = {
    if(_server != null) { 
      _server.shutdown() 
      _server = null
    }
  }
}
