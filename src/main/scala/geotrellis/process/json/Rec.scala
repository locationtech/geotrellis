package geotrellis.process.json

import geotrellis._
import geotrellis.process._

/**
 * Records are the raw scala/json objects, rather than the objects we
 * actually want to pass to the constructors.
 *
 * Rec[T] is expected to implement a create method which builds an
 * instance of T. Records are also required to have a name (which will be
 * used when building maps out of lists.
 */
trait Rec[T] {
  def name: String
}

case class CatalogRec(catalog:String,
                      stores:List[DataStoreRec]) extends Rec[Catalog] {
  def create(json:String, source:String) = 
    Catalog(catalog, stores.map(s => s.name -> s.create).toMap, json, source)
  def name = catalog
}

case class DataStoreRec(store:String,
                        params:Map[String, String],
                        catalogPath:String) extends Rec[DataStore] {
  def create = DataStore(store, params,catalogPath)
  def name = store
}
