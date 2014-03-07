package geotrellis

import geotrellis._
import geotrellis.process._
import geotrellis.source._

object GeoTrellis {
  private var _server:Server = null
  def server = {
    if(!isInit) {
      init()
    }
    _server
  }

  def isInit: Boolean = _server != null

  def init():Unit = init(GeoTrellisConfig())

  def init(config:GeoTrellisConfig, name:String = "geotrellis-server"):Unit = {
    if(_server!= null) {
      sys.error("GeoTrellis has already been initialized. You must only initiliaze once before shutdown.")
    }

    val catalog = config.catalogPath match {
      case Some(path) =>
        val file = new java.io.File(path)
        if(!file.exists()) {
          sys.error(s"Catalog path ${file.getAbsolutePath} does not exist. Please modify your settings.")
        }
        Catalog.fromPath(file.getAbsolutePath)
      case None => Catalog.empty(s"${name}-catalog")
    }
    _server = Server(name, catalog)
  }

  def run[T](op: Op[T]):OperationResult[T] = {
    server.run(op)
  }

  def run[T](source: DataSource[_,T]):OperationResult[T] = {
    server.run(source)
  }

  def get[T](op: Op[T]):T = {
    server.get(op)
  }

  def get[T](source: DataSource[_,T]):T = {
    server.get(source)
  }

  def shutdown() = {
    if(_server != null) { 
      _server.shutdown() 
      _server = null
    }
  }
}
