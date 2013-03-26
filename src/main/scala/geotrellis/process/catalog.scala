package geotrellis.process

import scala.collection.mutable
import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.process._
import geotrellis.util._
import geotrellis.util.Filesystem

// example json is available in the geotrellis.process.catalog tests. please
// keep it up-to-date with changes you make here.

/**
 * Represents a layer of raster data, or a feature layer (e.g. a postgis table
 * of polygons).
 */
trait Layer { 
  def name:String
}

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


/**
 * Represents a named collection of data stores. We expect each JSON file to
 * correspond to one catalog.
 */
case class Catalog(name:String, stores:Map[String, DataStore], json: String, source: String) { 

  def getRasterLayer(path:String): Option[RasterLayer] = {
    stores.values.flatMap(_.getRasterLayer(path)).headOption
  }

  def getRasterLayerByName(name:String):Option[RasterLayer] = {
    stores.values.flatMap(_.getRasterLayerByName(name)).headOption
  }
}

/**
 * Records are the raw scala/json objects, rather than the objects we
 * actually want to pass to the constructors.
 *
 * Record[T] is expected to implement a create method which builds an
 * instance of T. Records are also required to have a name (which will be
 * used when building maps out of lists.
 */
trait Rec[T] {
  def name: String
}

/**
 * Concrete Rec types defined below.
 */
case class RasterLayerRec(layer:String, `type`:String, datatype:String, 
                          xmin:Double, xmax:Double, ymin:Double, ymax:Double, 
                          cols:Int, rows:Int, cellheight:Double, cellwidth:Double, 
                          epsg:Int, yskew:Double, xskew:Double) extends Rec[RasterLayer] {
  def create(basePath:String) = {
    val extent = Extent(xmin, ymin, xmax, ymax)
    val rasterExtent = RasterExtent(extent, cellwidth, cellheight, cols, rows)
    RasterLayer(layer, `type`, datatype, basePath, rasterExtent, epsg, yskew, xskew)
  }
  def name = layer
}

case class DataStoreRec(store:String,
                        params:Map[String, String]) extends Rec[DataStore] {
  def create = DataStore(store, params)
  def name = store
}

case class CatalogRec(catalog:String,
                      stores:List[DataStoreRec]) extends Rec[Catalog] {
  def create(json:String, source:String) = Catalog(catalog, stores.map(s => s.name -> s.create).toMap, json, source)
  def name = catalog
}


object RasterLayer {
  /**
   * Build a RasterLayer instance given a path to a JSON file.
   */
  def fromPath(path:String) = {
    val base = Filesystem.basename(path)
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data, base)
  }

  def fromFile(f:File) = RasterLayer.fromPath(f.getAbsolutePath)

  /**
   * Build a RasterLayer instance given a JSON string.
   */
  def fromJSON(json:String, basePath:String) = {
    RasterLayerJson.parse(json).create(basePath)
  }
}

case class RasterLayer(name:String, typ:String, datatyp:String,
                       basePath:String, rasterExtent:RasterExtent,
                       epsg:Int, xskew:Double, yskew:Double) extends Layer {
  def jsonPath = basePath + ".json"
  def rasterPath = basePath + "." + typ
}

object Catalog {
  /**
   * Build a Catalog instance given a path to a JSON file.
   */
  def fromPath(path:String): Catalog = {
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    CatalogJson.parse(data).create(data, path)
  }

  /**
   * Build a Catalog instance given a string of JSON data.
   */
  def fromJSON(data:String): Catalog = CatalogJson.parse(data).create(data, "unknown")

  /**
   * Builds an empty Catalog.
   */
  def empty(name:String) = Catalog(name, Map.empty[String, DataStore], "{}", "empty()" )
}
