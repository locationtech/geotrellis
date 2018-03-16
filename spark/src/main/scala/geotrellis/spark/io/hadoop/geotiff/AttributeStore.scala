package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector.ProjectedExtent

trait CollectionAttributeStore[T] extends AttributeStore[Seq, T]
trait IteratorAttributeStore[T] extends AttributeStore[Iterator, T]

/** Layer that works with Metadata Index ?? */
trait AttributeStore[M[_], T] {
  /**
    * The only one query that has to be implemented with this interface
    * We are going to check this theory by implementing PSQL AttributeStore
    * */
  def query(layerName: Option[String], extent: Option[ProjectedExtent]): M[T]

  def query(layerName: String, extent: ProjectedExtent): M[T] =
    query(Some(layerName), Some(extent))

  def query(layerName: String): M[T] =
    query(Some(layerName), None)

  def query(extent: ProjectedExtent): M[T] =
    query(None, Some(extent))

  def query: M[T] = query(None, None)
}
