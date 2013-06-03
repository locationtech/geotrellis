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
    val skipDirectories = mutable.Set[String]()
    for(f <- d.listFiles
              .filter(_.isFile)
              .filter(_.getPath.endsWith(".json"))) {
      // It's a JSON file
      // which may contain layer metadata,
      // or we just ignore it.
      RasterLayer.fromFile(f) match {
        case Some(layer) =>
          layers(layer.info.name) = layer
          // Skip the tile directory if it's a tiled raster.
          layer match {
            case tl:TileSetRasterLayer =>
              skipDirectories.add(new File(tl.tileDirPath).getAbsolutePath)
            case _ =>
          }
        case _ =>
          System.err.println(s"Skipping ${f.getPath}...")
      }
    }

    // Recurse through subdirectories. If a directory was marked
    // as containing a tile set, skip it.
    for(subdir <- d.listFiles
                   .filter(_.isDirectory)
                   .filter(f => !skipDirectories.contains(f.getAbsolutePath))) {
      initDirectory(subdir)
    }
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
