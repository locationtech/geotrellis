package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.RasterData
import geotrellis.ArrayRaster
import geotrellis.GeoAttrsError

/**
 * Implementation to find the Nth maximum element of a set of rasters for each cell.
 * Uses a randomized in-place quick select algorithm that was found to be the fastest on average in benchmarks.
 * @author jchien
 */
object MaxN extends Serializable {

  case class ArrayView[Num](arr: Array[Num], from: Int, until: Int) {
    def apply(n: Int) =
      if (from + n < until) arr(from + n)
      else throw new ArrayIndexOutOfBoundsException(n)

    def partitionInPlace(p: Num => Boolean): (ArrayView[Num], ArrayView[Num]) = {
      var upper = until - 1
      var lower = from
      while (lower < upper) {
        while (lower < until && p(arr(lower))) lower += 1
        while (upper >= from && !p(arr(upper))) upper -= 1
        if (lower < upper) { val tmp = arr(lower); arr(lower) = arr(upper); arr(upper) = tmp }
      }
      (copy(until = lower), copy(from = lower))
    }

    def size = until - from
    def isEmpty = size <= 0
  }
  object ArrayView {
    def apply(arr: Array[Double]) = new ArrayView(arr, 0, arr.size)
    def apply(arr: Array[Int]) = new ArrayView(arr, 0, arr.size)
  }

  def findNthIntInPlace(arr: ArrayView[Int], n: Int): Int = {
    if(n >= arr.size) {
      NODATA
    }else {
      val pivot = arr(scala.util.Random.nextInt(arr.size))
      val (left, right) = arr partitionInPlace (_ > pivot)
      if (left.size == n) pivot
      else if (left.isEmpty) {
        val (left, right) = arr partitionInPlace (_ == pivot)
        if (left.size > n) pivot
        else findNthIntInPlace(right, n - left.size)
      } else if (left.size < n) findNthIntInPlace(right, n - left.size)
      else findNthIntInPlace(left, n)
    }
  }

  def findNthDoubleInPlace(arr: ArrayView[Double], n: Int): Double = {
    if(n >= arr.size) {
      Double.NaN
    }else {
      val pivot = arr(scala.util.Random.nextInt(arr.size))
      val (left, right) = arr partitionInPlace (_ > pivot)
      if (left.size == n) pivot
      else if (left.isEmpty) {
        val (left, right) = arr partitionInPlace (_ == pivot)
        if (left.size > n) pivot
        else findNthDoubleInPlace(right, n - left.size)
      } else if (left.size < n) findNthDoubleInPlace(right, n - left.size)
      else findNthDoubleInPlace(left, n)
    }
  }

  def apply(n:Int,rs:Raster*):Raster =
    apply(n,rs)

  def apply(n:Int,rs:Seq[Raster])(implicit d:DI):Raster = {
    if(Set(rs.map(_.rasterExtent)).size != 1) {
      val rasterExtents = rs.map(_.rasterExtent).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
        s"$rasterExtents are not all equal")
    }

    val layerCount = rs.length
    if(layerCount < n) {
      sys.error(s"Not enough values to compute Nth")
    } else {
      val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
      val re = rs(0).rasterExtent
      val cols = re.cols
      val rows = re.rows
      val data = RasterData.allocByType(newRasterType,cols,rows)

      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          if(newRasterType.isDouble) {
            val maxN = findNthDoubleInPlace(ArrayView(rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)).toArray), n)
            data.setDouble(col, row, maxN)
          }else { // integer values
            val maxN = findNthIntInPlace(ArrayView(rs.map(r => r.get(col,row)).filter(num => !isNoData(num)).toArray), n)
            data.set(col, row, maxN)
          }
        }
      }
      ArrayRaster(data,re)
    }
  }
}