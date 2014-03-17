package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.RasterData
import geotrellis.source.RasterSource
import geotrellis.ArrayRaster
import geotrellis.GeoAttrsError

/**
 * Created by jchien on 2/9/14.
 */
object MinN extends Serializable {

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
    } else {
      val pivot = arr(scala.util.Random.nextInt(arr.size))
      val (left, right) = arr partitionInPlace (_ < pivot)
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
    } else {
      val pivot = arr(scala.util.Random.nextInt(arr.size))
      val (left, right) = arr partitionInPlace (_ < pivot)
      if (left.size == n) pivot
      else if (left.isEmpty) {
        val (left, right) = arr partitionInPlace (_ == pivot)
        if (left.size > n) pivot
        else findNthDoubleInPlace(right, n - left.size)
      } else if (left.size < n) findNthDoubleInPlace(right, n - left.size)
      else findNthDoubleInPlace(left, n)
    }
  }

  def quickSelectInt(seq: Seq[Int], n: Int): Int = {
    if (n >= seq.length) {
      NODATA
    } else if (n == 0) {
      seq.min
    } else {
      val pivot = seq(scala.util.Random.nextInt(seq.length))
      val (left, right) = seq.partition(_ < pivot)
      if (left.length == n) {
        pivot
      } else if (left.length == 0){
        val (left, right) = seq.partition(_ == pivot)
        if (left.length > n) pivot
        else quickSelectInt(right, n - left.length)
      } else if (left.length < n) {
        quickSelectInt(right, n - left.length)
      } else {
        quickSelectInt(left, n)
      }
    }
  }

  def quickSelectDouble(seq: Seq[Double], n: Int): Double = {
    if (n >= seq.length) {
      Double.NaN
    } else if (n == 0) {
      seq.min
    } else {
      val pivot = seq(scala.util.Random.nextInt(seq.length))
      val (left, right) = seq.partition(_ < pivot)
      if (left.length == n) {
        pivot
      } else if (left.length == 0){
        val (left, right) = seq.partition(_ == pivot)
        if (left.length > n) pivot
        else quickSelectDouble(right, n - left.length)
      } else if (left.length < n) {
        quickSelectDouble(right, n - left.length)
      } else {
        quickSelectDouble(left, n)
      }
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
            //val minN = findNthDoubleInPlace(ArrayView(rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)).toArray), n)
            val minN = quickSelectDouble(rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)), n)
            data.setDouble(col, row, minN)
          }else { // integer values
          //val minN = findNthIntInPlace(ArrayView(rs.map(r => r.get(col,row)).filter(num => !isNoData(num)).toArray), n)
          val minN = quickSelectInt(rs.map(r => r.get(col,row)).filter(num => !isNoData(num)), n)
            data.set(col, row, minN)
          }
        }
      }
      ArrayRaster(data,re)
    }
  }
}
