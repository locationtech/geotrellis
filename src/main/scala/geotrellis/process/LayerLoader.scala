package geotrellis.process

import geotrellis.process._
import geotrellis.util.Filesystem

/** LayerLoader will be passed into Operation[T]'s that
  * mix in the LayerOp trait right before 'run' is called
  * on the operation step and cleared afterwards.
  */
class LayerLoader(server:Server) {
  /**
   * Clients can call the raster path loading functions
   * with either the .json metadata (prefered) or with
   * the .arg extension. This function moves the latter
   * path into the metadata path.
   */
  def processPath(path:String):String =
    if(path.endsWith(".arg")) {
      path.substring(0,path.length - 4) + ".json"
    } else if (path.endsWith("/")) {
      path.substring(0,path.length - 1) + ".json"
    } else if (!path.endsWith(".json")){
      path + ".json"
    } else {
      path
    }

  /**
   * Load RasterLayer from the catalog.
   */
  def getRasterLayer(layerId:LayerId): RasterLayer =
    server.catalog.getRasterLayer(layerId) match {
      case Some(layer) => layer
      case None => sys.error(s"couldn't find raster ${layerId.name} in catalog at ${server.catalog.source}")
    }

  /**
   * Load RasterLayer from the catalog from a path.
   */
  def getRasterLayerFromPath(path:String):RasterLayer =
    RasterLayer.fromPath(processPath(path)) match {
      case Some(layer) => layer
      case None => sys.error(s"couldn't load raster at ${processPath(path)}")
    }

  /**
   * Load RasterLayer from the catalog from a url.
   */
  def getRasterLayerFromUrl(url:String):RasterLayer = 
    RasterLayer.fromUrl(url) match {
      case Some(layer) => layer
      case None => sys.error(s"couldn't get raster layer from URL $url")
    }
}
