package geotrellis.spark

import geotrellis.spark._
import org.apache.spark.rdd.RDD

/** A GridKey designates the spatial positioning of a layer's tile. */
case class GridKey(col: Int, row: Int) extends Product2[Int, Int] {
  def _1 = col
  def _2 = row
}

object GridKey {
  implicit def tupToKey(tup: (Int, Int)): GridKey =
    GridKey(tup._1, tup._2)

  implicit def keyToTup(key: GridKey): (Int, Int) =
    (key.col, key.row)

  implicit def ordering[A <: GridKey]: Ordering[A] =
    Ordering.by(sk => (sk.col, sk.row))

  implicit object Boundable extends Boundable[GridKey] {
    def minBound(a: GridKey, b: GridKey) = {
      GridKey(math.min(a.col, b.col), math.min(a.row, b.row))
    }
    def maxBound(a: GridKey, b: GridKey) = {
      GridKey(math.max(a.col, b.col), math.max(a.row, b.row))
    }
  }
}
