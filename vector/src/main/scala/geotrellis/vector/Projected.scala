package geotrellis.vector

import geotrellis.proj4.CRS

object Projected {
  implicit def fromTupleA[T](tup: (T, CRS)):ProjectedExtent = Projected(tup._1, tup._2)
  implicit def fromTupleB[T](tup: (CRS, T)):ProjectedExtent = Projected(tup._2, tup._1)
}

case class Projected[T](obj: T, crs: CRS) extends Product2[T, CRS]