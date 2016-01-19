package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster._
import geotrellis.raster.op.local._
import org.apache.spark.rdd.RDD
import scala.reflect._

abstract class LocalRasterRDDSeqMethods[K: ClassTag] extends MethodExtensions[Traversable[RDD[(K, Tile)]]] {
  private def r(f: Traversable[Tile] => (Tile)) =
    self.headOption match {
      case Some(head) => head.combineValues(self.tail)(f)
      case None => sys.error("raster rdd operations can't be applied to empty seq!")
    }

  def localAdd = r { Add.apply }

  /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety = r { Variety.apply }

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean = r { Mean.apply }

  def localMin = r { Min.apply }

  def localMinN(n: Int) = r { MinN(n, _) }

  def localMax = r { Max.apply }

  def localMaxN(n: Int) = r { MaxN(n, _) }

  def localMinority(n: Int = 0) = r { Minority(n, _) }

  def localMajority(n: Int = 0) = r { Majority(n, _) }
}