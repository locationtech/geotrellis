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

object Catalog {
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
