package geotrellis.spark

import geotrellis.spark.tiling.TileCoordScheme
import geotrellis.spark._

import scala.collection.mutable

trait KeyFilter

trait Filterable[K, KF <: KeyFilter]

case class SpaceFilter(val bounds: TileBounds, val scheme: TileCoordScheme) extends KeyFilter

case class TimeFilter(val startTime: Double, val endTime: Double) extends KeyFilter

class FilterSet[K] {
  private val _filters = mutable.ListBuffer[KeyFilter]()

  def withFilter[K, KF <: KeyFilter](filter: KF)(implicit ev: Filterable[K, KF]) = {
    _filters += filter
    this
  }

  def filters: Seq[KeyFilter] = _filters

}

object FilterSet{
  def apply[K]() = new FilterSet[K]
}


//object thing {
//  val filters =
//    FilterSet[TimeTileId]()
//    .withFilter(SpaceFilter(???, ???))
//    .withFilter(TimeFilter(???, ???))
//}