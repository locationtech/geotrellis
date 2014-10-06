package geotrellis.spark

import geotrellis.spark.tiling.TileCoordScheme
import geotrellis.spark._
import org.joda.time.DateTime

import scala.annotation.implicitNotFound
import scala.collection.mutable

trait KeyFilter

@implicitNotFound("Filterable[K,F] instance required in implicit scope to associate a filter type F with a key type K")
trait Filterable[K, F]

case class SpaceFilter(bounds: TileBounds, scheme: TileCoordScheme) extends KeyFilter
case class TimeFilter(startTime: DateTime, endTime: DateTime) extends KeyFilter

class FilterSet[K] {
  private var _filters = mutable.ListBuffer[KeyFilter]()

  def withFilter[F <: KeyFilter](filter: F)(implicit ev: Filterable[K, F]) = {
    _filters += filter
    this
  }
  def filters: Seq[KeyFilter] = _filters
}

object FilterSet{
  def apply[K]() = new FilterSet[K]
}


