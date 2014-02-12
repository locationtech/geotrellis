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
trait LocalBinaryOpMethods[+Repr <: RDD[TileIdRaster]] { self: Repr =>

  def mapOp(d: Double, f: (Raster, Double) => Raster) =
    self.mapPartitions(_.map(tr => (tr._1, f(tr._2, d))))

}

trait AddOpMethods[+Repr <: RDD[TileIdRaster]] extends LocalBinaryOpMethods[Repr] { self: Repr =>
  /** Add a constant Int value to each cell. */
  //def localAdd(i: Int) = self.mapOp(Add(_, i))
  /** Add a constant Int value to each cell. */
  //def +(i:Int) = localAdd(i)
  /** Add a constant Int value to each cell. */
  //def +:(i:Int) = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) = self.mapOp(d, (r, d) => Add(r, d))
  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)
  /** Add the values of each cell in each raster.  */
  //def localAdd(rs:RasterSource) = self.combineOp(rs)(Add(_,_))
  /** Add the values of each cell in each raster. */
  //def +(rs:RasterHadoopRDD) = localAdd(rs)
}
