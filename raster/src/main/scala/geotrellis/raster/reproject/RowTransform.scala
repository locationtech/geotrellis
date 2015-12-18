package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

object RowTransform {
  /** Transform each point exactly. Assumes user has taken care to make all array dimensions equal. */
  def exact(transform: Transform): RowTransform =
    { (srcX: Array[Double], srcY: Array[Double], destX: Array[Double], destY: Array[Double]) =>
      cfor(0)(_ < srcX.length, _ + 1) { i =>
        val (x, y) = transform(srcX(i), srcY(i))
        destX(i) = x
        destY(i) = y
      }
    }

  /** Computes an approximate transformation of a row.
    * 
    * Based on GDALApproxTransform. Assumes that the number of elements in the row is greater than 5.
    * Should only be used when the line being transformed is roughly linear.
    * 
    * @param transform                   The transformation that we are approximating
    * @param errorThreshold              maximum error measured in input pixels that is allowed
    *                                    in approximating the transformation (0.0 for exact calculations).
    * 
    * @note This algorithm does not garuntee all values will be within the error threshold of the exactly transformed values.
    *       In practice, as long as the line is roughly linear, it should be at least very close if over the error threshold away
    *       from the actual values.
    */
  def approximate(transform: Transform, errorThreshold: Double): RowTransform =
    if(errorThreshold == 0.0) {
      exact(transform)
    } else {
      { (srcX: Array[Double], srcY: Array[Double], destX: Array[Double], destY: Array[Double]) =>
        // Reproject first and last points
        val len = srcX.length
        val (xmin, ymin) = transform(srcX(0), srcY(0))
        val (xmax, ymax) = transform(srcX(len - 1), srcY(len - 1))
        destX(0) = xmin
        destY(0) = ymin
        destX(len - 1) = xmax
        destY(len - 1) = ymax

        computeApprox(transform, errorThreshold, srcX, srcY, destX, destY, 0, len)
      }
    }

  private def computeApprox(
    transform: Transform, errorThreshold: Double,
    srcX: Array[Double], srcY: Array[Double], destX: Array[Double], destY: Array[Double],
    startIndex: Int, length: Int): Unit = {

    if(length == 2) return

    val midPoint = startIndex + ((length - 1) / 2)
    val (xmid, ymid) = transform(srcX(midPoint), srcY(midPoint))

    destX(midPoint) = xmid
    destY(midPoint) = ymid

    if(length != 3) {
      val srcXMax = srcX(startIndex + length - 1)
      val srcXMin = srcX(startIndex)
      val xmax = destX(startIndex + length - 1)
      val xmin = destX(startIndex)
      val ymax = destY(startIndex + length - 1)
      val ymin = destY(startIndex)

      val dx = srcXMax - srcXMin
      val deltaX = (xmax - xmin) / dx
      val deltaY = (ymax - ymin) / dx

      val dxmid = srcX(midPoint) - srcXMin
      val error = math.abs( (xmin + (deltaX * dxmid)) - xmid) +
                  math.abs( (ymin + (deltaY * dxmid)) - ymid)

      if(error > errorThreshold) {
        // Compute against the two halves of the row
        computeApprox(transform, errorThreshold, srcX, srcY, destX, destY,
          startIndex, midPoint - startIndex + 1)
        computeApprox(transform, errorThreshold, srcX, srcY, destX, destY,
          midPoint, startIndex + length - midPoint)
      } else {
        // Fill out the values based on the linear interpolation
        cfor(startIndex + 1)(_ < startIndex + length - 1, _ + 1) { i =>
          if(i != midPoint) {
            val dxi = srcX(i) - srcXMin
            destX(i) = xmin + (deltaX * dxi)
            destY(i) = ymin + (deltaY * dxi)
          }
        }
      }
    }
  }
}
