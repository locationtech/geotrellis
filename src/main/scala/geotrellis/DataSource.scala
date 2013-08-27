package geotrellis

import scala.collection._
import scala.collection.mutable.{ArrayBuffer,ListBuffer, Builder}
import scala.collection.generic._
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.SetBuilder
import scala.collection.immutable.TreeSet
import scala.annotation.unchecked.uncheckedVariance
import scala.language.higherKinds
import geotrellis.raster.op._
import geotrellis._

/**
 * Represents a data source that may be distributed across machines (logical data source) 
 * or loaded in memory on a specific machine. 
  */
trait DataSource[T,P] extends DataSourceLike[T,P,DataSource[T,P]] {
  // def partitions():Op[Seq[Op[P]]]
  // def get:Op[T]
}

trait Convergable[T] {
  def get:Op[T]
}

trait Partitioned[P] {
  def partitions():Op[Seq[Op[P]]]
}
