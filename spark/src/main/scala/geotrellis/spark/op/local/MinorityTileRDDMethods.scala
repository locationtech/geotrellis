package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster._
import geotrellis.raster.op.local.Minority
import org.apache.spark.Partitioner

trait MinorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[Self]): Self = localMinority(others, None)
  def localMinority(others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    self.combineValues(others, partitioner)(Minority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: Self*)(implicit d: DI): Self =
    localMinority(rs, None)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Traversable[Self]): Self = localMinority(n, others, None)
  def localMinority(n: Int, others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    self.combineValues(others, partitioner) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: Self*)(implicit d: DI): Self =
    localMinority(n, rs, None)
}
