package geotrellis.spark.op.local
import geotrellis.Raster
import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import org.apache.spark.Logging

trait LocalBinaryOpMethods[+Repr <: RasterRDD] extends Logging { self: Repr =>

  // map over a rdd with a function f that takes a raster and something and produces a raster
  def mapOp[T](d: T)(f: (Tile, T) => Tile) =
    withContext(self.opCtx) {
      self.mapPartitions({ partition =>
        partition.map { tile =>
          f(tile, d)
        }
      }, true)
    }

  // function takes two rasters and produces a third raster  
  def combineOp(other: RasterRDD)(f: ((Tile, Tile)) => Tile) =
    self.zipPartitions(other, true)(_.zip(_).map(f)).withContext(self.opCtx)

}
