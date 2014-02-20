package geotrellis.spark.op.local

import geotrellis.spark.rdd.RasterHadoopRDD
import geotrellis.source.RasterSource
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.Raster
import org.apache.spark.rdd.RDD
import geotrellis.spark.tiling.TileIdRaster
import geotrellis.spark._
import geotrellis.raster.op.local.Add
import org.apache.spark.Logging
import geotrellis.spark.rdd.RasterRDD
trait LocalBinaryOpMethods[+Repr <: RasterRDD] extends Logging { self: Repr =>

  // map over a rdd with a function f that takes a raster and something and produces a raster
  def mapOp[T](d: T)(f: ((Long, Raster), T) => (Long, Raster)) =
    self.mapPartitions(_.map(f(_, d)), true)

  // function takes two rasters and produces a third raster  
  def combineOp(other: RasterRDD)(f: (((Long, Raster), (Long, Raster))) => (Long, Raster)) =
    self.zipPartitions(other, true)(_.zip(_).map(f))

}

trait AddOpMethods[+Repr <: RasterRDD] extends LocalBinaryOpMethods[Repr] { self: Repr =>
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) = 
    self.mapOp[Int](i)({ case ((t, r), i) => (t, Add(r, i)) }).withMetadata(self.meta)
  /** Add a constant Int value to each cell. */
  def +(i: Int) = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i: Int) = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) = 
    self.mapOp[Double](d)({ case ((t, r), d) => (t, Add(r, d)) }).withMetadata(self.meta)
  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(rdd: RasterRDD) =
    self.combineOp(rdd)({ case ((t1, r1), (t2, r2)) => (t1, Add(r1, r2)) }).withMetadata(self.meta)
  /** Add the values of each cell in each raster. */
  def +(rdd: RasterRDD) = localAdd(rdd)
}
