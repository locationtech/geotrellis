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

// example json is available in the geotrellis.process.catalog tests. please
// keep it up-to-date with changes you make here.

/**
 * Represents a named collection of data stores. We expect each JSON file to
 * correspond to one catalog.
 */
case class Catalog(name:String, stores:Map[String, DataStore], json: String, source: String) {
  val cache = new HashCache()

  private var cacheSet = false
  def initCache() =
    if(!cacheSet) {
      stores.values
            .filter(_.hasCacheAll)
            .map(_.cacheAll)
      cacheSet = true
    }

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
      "tiled" -> TileSetRasterLayerBuilder
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
    json.CatalogParser(data).create(data, path)
  }

  /**
   * Build a Catalog instance given a string of JSON data.
   */
  def fromJSON(data:String): Catalog = json.CatalogParser(data).create(data, "unknown")

  /**
   * Builds an empty Catalog.
   */
  def empty(name:String) = Catalog(name, Map.empty[String, DataStore], "{}", "empty()" )
}
