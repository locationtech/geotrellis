package trellis.process

import scala.collection.mutable.{Map => MMap}
import scala.io.Source
import scala.util.matching.Regex
import java.io.File

import net.liftweb.json.{parse, DefaultFormats}

import trellis.{Extent,RasterExtent}
import trellis.process._
import trellis.IntRaster

// this is a work in progress. the definitions for Layer, DataStore and Catalog
// are not complete, and we will likely need more objects.
//
// example json is available in the trellis.process.catalog tests. please keep
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
    case (path, layer) => {
      layers(layer.name) = (path, layer)
      paths(path) = layer
    }
  }

  def getLayer(name:String) = layers(name)
  
  //def getRaster(name:String, server:Server) = {}

  /**
   * Recursively find RasterLayers defined in the DataStore path.
   */
  def findRasterLayers: Array[(String, RasterLayer)] = {
    val path = new java.io.File(params("path"))
    if (!path.isDirectory) return Array.empty[(String, RasterLayer)]

    val layerJson = recursiveListFiles(path, """^.*\.json$""".r)
    layerJson map {
      x => (x.getPath, RasterLayer.fromPath(x.getPath))
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
  def getRasterLayerByName(layerName:String):Option[(String, RasterLayer)] = {
    stores.values.foreach {
      store => {
        if (store.layers.contains(layerName)) {
          val (jsonPath, layer) = store.layers(layerName)
  
          // fixme
          val path = jsonPath.substring(0, jsonPath.length - 5) + ".arg32"
          return Some((path, layer))
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
  def create: T

  /**
   * Helper method for transforming a list of records into a map of objects.
   */
  def recsToMap[T](recs:List[Rec[T]]): Map[String, T] = {
    recs.map(r => r.name -> r.create).toMap
  }
}

/**
 * Concrete Rec types defined below.
 */
case class RasterLayerRec(layer:String, xmin:Double, xmax:Double, ymin:Double,
                          ymax:Double, cols:Int, rows:Int,
                          cellheight:Double, cellwidth:Double) extends Rec[RasterLayer] {
  def create = {
    val extent = Extent(xmin, ymin, xmax, ymax)
    val rasterExtent = RasterExtent(extent, cellwidth, cellheight, cols, rows)
    RasterLayer(layer, rasterExtent)
  }
  def name = layer
}

case class DataStoreRec(store:String,
                        params:Map[String, String]
                        ) extends Rec[DataStore] {
  def create = DataStore(store, params)
  def name = store
}

case class CatalogRec(catalog:String,
                      stores:List[DataStoreRec]) extends Rec[Catalog] {
  def create = Catalog(catalog, recsToMap(stores))
  def name = catalog
}


object RasterLayer {
  implicit val formats = DefaultFormats

  /**
   * Build a RasterLayer instance given a path to a JSON file.
   */
  def fromPath(path:String) = {
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data)
  }

  /**
   * Build a RasterLayer instance given a JSON string.
   */
  def fromJSON(json:String) = parse(json).extract[RasterLayerRec].create
}

case class RasterLayer(name:String, rasterExtent:RasterExtent) extends Layer {
  def buildRaster(): IntRaster = sys.error("unimplemented")
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
