package geotrellis.process

import scala.collection.mutable
import java.io.File
import geotrellis._
import geotrellis.util.Filesystem

/**
 * Represents a location where data can be loaded from (e.g. the filesystem,
 * postgis, a web service, etc).
 */
case class DataStore(name:String, params:Map[String, String]) {

  private val layers = mutable.Map.empty[String, RasterLayer]

  initRasterLayers()

  /**
   * Initialize raster layers from the directory specified by the 'path' param.
   */
  private def initRasterLayers() {
    val path = params("path")
    val f = new File(path)
    if (!f.isDirectory) {
      sys.error("store %s is not a directory" format path)
    }

    // Walk the directory to for raster layers;
    // also search subdirectories, but some directories
    // might be tiled rasters.
    initDirectory(f)
  }

  private def initDirectory(d:File) {
    val files = d.listFiles.filter(f => f.isFile)
    val skipDirectories = mutable.Set[File]()
    for(f <- files) {

    }

    for(child <- d.listFiles) {

      if(child.isDirectory) {
        // It's a directory, so either it's a Tiled layer
        // or it's a subdirectory that we will search.
        if(isTileDirectory(child)) {
          loadTileLayer(child)
        } else {
          initDirectory(child)
        }

      } else {
        // It's a file, so either it's a JSON file
        // which may or may not contain layer metadata,
        // or we just ignore it.
        if(child.getPath.endsWith(".json")) {
          RasterLayer.fromFile(child) match {
            case Some(layer) =>
              layers(layer.info.name) = layer
            case _ =>
              System.err.println(s"Skipping ${child.getPath}...")
          }
        }
      }
    }
  }

  /*
   *  Assumes that it is a directory
   */
  private def isTileDirectory(f:File) = {
    val layout = new File(f, "layout.json")
    layout.exists
  }

  private def loadTileLayer(f:File) = {

  }

  def cacheAll() =
    layers.values
          .map(_.cache)

  def getNames = layers.keys
  def getLayers = layers.values

  def hasCacheAll = if(params.contains("cacheAll")) {
    val value = params("cacheAll").toLowerCase
    value == "true" || value == "yes" || value == "1"
  } else { false }

  def getRasterLayerByName(name:String):Option[RasterLayer] = {
    layers.get(name)
  }
}
