package geotrellis.source

import scala.collection._
import scala.collection.generic._
import geotrellis.raster.op._
import geotrellis._
import geotrellis.statistics._

/**
 * Represents a data source that may be distributed across machines (logical data source) 
 * or loaded in memory on a specific machine. 
  */
trait DataSource[+T,+V] extends DataSourceLike[T,V,DataSource[T,V]] {
}
