package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._

/**
  * Generate quantile class breaks for a given raster.
  */
object GetClassBreaks {
  def apply(r:Op[Raster], n:Op[Int]):Op[Array[Int]] =
    apply(GetHistogram(r),n)

  def apply(h:Op[Histogram], n:Op[Int])(implicit d:DI):Op[Array[Int]] =
    (h,n).map { (h,n) =>
            h.getQuantileBreaks(n)
          }
         .withName("GetClassBreaks")
}
