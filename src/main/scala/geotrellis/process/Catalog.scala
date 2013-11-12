package geotrellis.process

import scala.collection.mutable
import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.process._
import geotrellis.util._
import geotrellis.util.Filesystem
import geotrellis.data.AsciiRasterLayerBuilder

import com.typesafe.config.Config

import java.io.File

// example json is available in the geotrellis.process.catalog tests. please
// keep it up-to-date with changes you make here.

/**
 * Represents a named collection of data stores. We expect each JSON file to
 * correspond to one catalog.
 */
case class Catalog(name:String, stores:Map[String, DataStore], json: String,source: String) {

  private var cacheSet = false
  def initCache(cache:Option[Cache]):Unit =
    if(!cacheSet) {
      // Set the cache on all layers.
      for(store <- stores.values) { store.setCache(cache) }

      // Cache all layers if the DataStore has cacheAll set
      stores.values
            .filter(_.hasCacheAll)
            .map(_.cacheAll)

      // If the DataStore didn't cache due to the cacheAll flag,
      // find any layer that has the cache flag set and cache those layers.
      stores.values
            .filter(!_.hasCacheAll)
            .map(_.getLayers)
            .flatten
            .filter(_.info.shouldCache)
            .map(_.cache)

      cacheSet = true
    } else {
      sys.error("Cache has already been set for this Catalog. " +
                "You may not set the cache more than once during a Catalog's lifetime.")
    }

  def initCache(cache:Cache):Unit = initCache(Some(cache))

  def getRasterLayerByName(name:String):Option[RasterLayer] = {
    stores.values.flatMap(_.getRasterLayerByName(name)).headOption
  }
}

object Catalog {
  private val stringToRasterLayerBuilder = 
    mutable.Map[String,RasterLayerBuilder](
      "constant" -> ConstantRasterLayerBuilder,
      "ascii" -> AsciiRasterLayerBuilder,
      "arg" -> ArgFileRasterLayerBuilder,
      "tiled" -> TileSetRasterLayerBuilder,
      "argurl" -> ArgUrlRasterLayerBuilder
    )

  def addRasterLayerBuilder(layerType:String,builder:RasterLayerBuilder) =
    if(stringToRasterLayerBuilder.contains(layerType)) {
      sys.error(s"A raster layer builder is already registered for the layer type '$layerType'")
    } else {
      stringToRasterLayerBuilder(layerType) = builder
    }

  def getRasterLayerBuilder(layerType:String):Option[RasterLayerBuilder] = 
    if(stringToRasterLayerBuilder.contains(layerType)) {
      Some(stringToRasterLayerBuilder(layerType))
    } else {
      None
    }

  /**
   * Build a Catalog instance given a path to a JSON file.
   */
  def fromPath(path:String): Catalog = {
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data,path)
  }

  def fromJSON(data:String):Catalog = fromJSON(data,"")

  /**
   * Build a Catalog instance given a string of JSON data.
   */
  def fromJSON(data:String,path:String): Catalog = 
    json.CatalogParser(data,path).create(data,path)

  /**
   * Builds an empty Catalog.
   */
  def empty(name:String) = Catalog(name, Map.empty[String, DataStore], "{}","empty()")
}
