package geotrellis.process

import scala.collection.mutable.{Map => MMap}
import scala.io.Source
import scala.util.matching.Regex
import java.io.File

import net.liftweb.json.{parse, DefaultFormats}

import geotrellis._
import geotrellis.process._
import geotrellis.util._

// this is a work in progress. the definitions for Layer, DataStore and Catalog
// are not complete, and we will likely need more objects.
//
// example json is available in the geotrellis.process.catalog tests. please keep
// it up-to-date with changes you make here.

/**
 * Represents a layer of raster data, or a feature layer (e.g. a postgis table
 * of polygons.
 */
trait Layer { 
  def name:String
}

/**
 * Represents a location where data can be loaded from (e.g. the filesystem,
 * postgis, a web service, etc).
 */
case class DataStore(name:String, params:Map[String, String]) {

  val layers = MMap.empty[String, (String, RasterLayer)]
  val paths = MMap.empty[String, RasterLayer]

  findRasterLayers.foreach {
    layer =>
    val path = layer.jsonPath
    layers(layer.name) = (path, layer)
    paths(path) = layer
  }

  def cacheAll = params.contains("cacheAll")

  def getLayer(name:String) = layers(name)

  /**
   * Recursively find RasterLayers defined in the DataStore path.
   */
  def findRasterLayers: Array[RasterLayer] = {
    val path = new java.io.File(params("path"))
    if (!path.isDirectory) return Array.empty[RasterLayer]

    val layerJson = recursiveListFiles(path, """^.*\.json$""".r)
    layerJson.map(_.getAbsolutePath).map {
      p => RasterLayer.fromPath(p)
      //p => (p, RasterLayer.fromPath(p))
    }
  }

  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good = these.filter(  _.getPath.endsWith(".json") )
    good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_,r))
  }

}


/**
 * Represents a named collection of data stores. We expect each JSON file to
 * correspond to one catalog.
 */
case class Catalog(name:String, stores:Map[String, DataStore]) {

  def findRasterLayer(path:String): Option[RasterLayer] = {
    stores.values.flatMap(_.paths.get(path)).headOption
  }

  private def exists(path:String) = if (new java.io.File(path).exists) Some(path) else None

  def findPath(base:String) = exists(base + ".arg32") orElse exists(base + ".arg")

  def getRasterLayerByName(layerName:String):Option[(String, RasterLayer)] = {
    stores.values.foreach {
      store => {
        if (store.layers.contains(layerName)) {
          val (jsonPath, layer) = store.layers(layerName)
  
          // fixme
          val base = jsonPath.substring(0, jsonPath.length - 5)
          return findPath(base).map(s => (s, layer))
          //val path = jsonPath.substring(0, jsonPath.length - 5) + ".arg32"
          //return Some((path, layer))
        }
      }
    }
    None
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
  def create = Catalog(catalog, stores.map(s => s.name -> s.create).toMap)
  def name = catalog
}


object RasterLayer {
  implicit val formats = DefaultFormats

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

  /**
   * Build a RasterLayer instance given a JSON string.
   */
  def fromJSON(json:String, basePath:String) = {
    parse(json).extract[RasterLayerRec].create(basePath)
  }
}

case class RasterLayer(name:String, typ:String, datatyp:String, basePath:String,
                       rasterExtent:RasterExtent, epsg:Int, xskew:Double, yskew:Double) extends Layer {
  def jsonPath = basePath + ".json"
  def rasterPath = basePath + "." + typ
}

object Catalog {

  /**
   * Enable Lift-JSON type extractions.
   */
  implicit val formats = DefaultFormats

  /**
   * Build a Catalog instance given a path to a JSON file.
   */
  def fromPath(path:String): Catalog = {
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data)
  }

  /**
   * Build a Catalog instance given a string of JSON data.
   */
  def fromJSON(data:String): Catalog = parse(data).extract[CatalogRec].create

  /**
   * Builds an empty Catalog.
   */
  def empty(name:String) = Catalog(name, Map.empty[String, DataStore])

}
