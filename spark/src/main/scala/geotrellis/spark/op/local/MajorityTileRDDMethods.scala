package geotrellis.spark.op.local

import scala.collection.immutable.Map

import geotrellis.raster._
import geotrellis.raster.op.local.Majority

import geotrellis.spark._
import geotrellis.spark.op._

trait MajorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(others: Traversable[Self]) =
    self.combineValues(others)(Majority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(rs: Self*): Self =
    localMajority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, others: Traversable[Self]) =
    self.combineValues(others) {
      tiles => Majority(n, tiles)
    }

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, rs: Self*): Self =
    localMajority(n, rs)
}
