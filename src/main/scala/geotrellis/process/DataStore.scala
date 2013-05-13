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
  private val paths = mutable.Map.empty[String, RasterLayer]

  initRasterLayers()

  /**
   * Initialize raster layers from the directory specified by the 'path' param.
   */
  private def initRasterLayers() {
    val path = params("path")
    val f = new java.io.File(path)
    if (!f.isDirectory) sys.error("store %s is not a directory" format path)
    find(f, ".json", initRasterLayer _)
  }

  /**
   * Initialize a raster layer from its JSON metadata file.
   */
  private def initRasterLayer(f:File) {
    val layer = RasterLayer.fromFile(f)
    layers(layer.name) = layer
    paths(layer.rasterPath) = layer
  }

  /**
   * Find files based on an extension. Directories will be searched recursively
   * and matching files will run the provided callback 'action'.
   */
  private def find(f:File, ext:String, action:File => Unit) {
    val fs = f.listFiles
    fs.filter(_.getPath.endsWith(ext)).foreach(f => action(f))
    fs.filter(_.isDirectory).foreach(f => find(f, ext, action))
  }

  def getNames = layers.keys
  def getPaths = paths.keys
  def getLayers = layers.values

  def hasCacheAll = if(params.contains("cacheAll")) {
    val value = params("cacheAll").toLowerCase
    value == "true" || value == "yes" || value == "1"
  } else { false }

  def getRasterLayer(path:String): Option[RasterLayer] = paths.get(path)

  def getRasterLayerByName(name:String):Option[RasterLayer] = {
    layers.get(name)
  }
}
