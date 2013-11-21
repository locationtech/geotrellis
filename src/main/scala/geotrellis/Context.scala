package geotrellis

import geotrellis.process._
import geotrellis.util.Filesystem

class Context(server:Server) {
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

  def loadRaster(path:String):Raster = 
    loadRaster(path, None)

  def loadRaster(path:String, re:RasterExtent):Raster = 
    loadRaster(path, Some(re))

  def loadRaster(path:String, reOpt:Option[RasterExtent]):Raster = 
    RasterLayer.fromPath(processPath(path)) match {
      case Some(layer) =>
        layer.getRaster(reOpt)
      case None =>
        sys.error(s"Cannot read raster layer at path $path")
    }

  def loadUncachedTileSet(path:String):Raster = 
    RasterLayer.fromPath(processPath(path)) match {
      case Some(layer) =>
        layer match {
          case tl:TileSetRasterLayer =>
            tl.getRaster
          case _ =>
            sys.error(s"Raster layer at path $path is not a tiled raster layer.")
        }
      case None => sys.error(s"Cannot load raster layer at path $path")
    }

  def getRasterStepOutput(path:String, reOpt:Option[RasterExtent]):StepOutput[Raster] = 
    RasterLayer.fromPath(processPath(path)) match {
      case Some(layer) => 
        Result(layer.getRaster(reOpt))
      case None =>
        StepError(s"Could not load raster from path: ${path}.","")
    }

  /**
   * Read a raster from a layer in the catalog.
   */
  def getRasterByName(name:String):StepOutput[Raster] = 
    getRasterByName(name,None)

  /**
   * Read a raster from a layer in the catalog with a specific extent.
   */
  def getRasterByName(name:String, re:RasterExtent):StepOutput[Raster] = 
    getRasterByName(name,Some(re))

  /**
   * Read a raster from a layer in the catalog.
   */
  def getRasterByName(name:String, reOpt:Option[RasterExtent]):StepOutput[Raster] = 
    server.catalog.getRasterLayerByName(name) match {
      case Some(layer) => {
        Result(layer.getRaster(reOpt))
      }
      case None => {
        val debugInfo = s"Failed to load raster ${name} from catalog at ${server.catalog.source}" + 
                        s" with json: \n ${server.catalog.json}"
        StepError(s"Did not find raster '${name}' in catalog", debugInfo)
      }
    }

  /**
   * Read a raster extent from a layer in the catalog
   */
  def getRasterExtentByName(name:String):RasterExtent = 
    server.catalog.getRasterLayerByName(name) match {
      case Some(layer) => layer.info.rasterExtent
      case None => sys.error(s"couldn't find raster $name in catalog at ${server.catalog.source}")
    }

  /**
   * Load RasterLayer from the catalog by name.
   */
  def getRasterLayer(name:String):RasterLayer =
    server.catalog.getRasterLayerByName(name) match {
      case Some(layer) => layer
      case None => sys.error(s"couldn't find raster $name in catalog at ${server.catalog.source}")
    }

  /**
   * Load RasterLayer from the catalog from a path.
   */
  def getRasterLayerFromPath(path:String):RasterLayer =
    RasterLayer.fromPath(processPath(path)) match {
      case Some(layer) => layer
      case None => sys.error(s"couldn't load raster at $path")
    }

  def getRasterLayerInfo(name:String):RasterLayerInfo =
    server.catalog.getRasterLayerByName(name) match {
      case Some(layer) => layer.info
      case None => sys.error(s"couldn't find raster $name in catalog at ${server.catalog.source}")
    }

  def getRasterLayerInfoFromPath(path:String):RasterLayerInfo =
    RasterLayer.fromPath(processPath(path)) match {
      case Some(layer) => layer.info
      case None => sys.error(s"couldn't get raster layer from path $path")
    }

  def getRasterUrl(url:String,re:Option[RasterExtent]):Raster = 
    RasterLayer.fromUrl(url) match {
      case Some(layer) => layer.getRaster(re)
      case None => sys.error(s"couldn't get raster layer from URL $url")
    }
}
