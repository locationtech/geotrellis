package geotrellis.benchmark

import geotrellis._
import geotrellis.raster._
import geotrellis.source._
import geotrellis.raster.op.local._

import com.google.caliper.Param
import scala.collection.mutable.ArrayBuffer

object MinNImplementation extends BenchmarkRunner(classOf[MinNImplementation])
class MinNImplementation extends OperationBenchmark {
//  @Param(Array("64", "128", "256", "512", "1024", "2048", "4096"))
  @Param(Array("64", "512", "1024"))
  var size:Int = 0

  var quickSelect: Raster = null
  var inPlace: Raster = null
  var array: Raster = null

  override def setUp() {
    val r = loadRaster("SBN_farm_mkt", size, size)
    val r1 = (r+1)
    val r2 = (r+2)
    val r3 = (r+3)
    val r4 = (r+4)
    val r5 = (r+5)

    quickSelect = MinN(2, r1, r2, r3, r4, r5)
    inPlace = InPlaceMinN(2, r1, r2, r3, r4, r5)
    array = ArrayMinN(2, r1, r2, r3, r4, r5)
  }

  def timeMinNInPlace(reps:Int) = run(reps)(minNInPlace)
  def minNInPlace = get(inPlace)

  def timeMinNQuickSelect(reps:Int) = run(reps)(minNQuickSelect)
  def minNQuickSelect = get(quickSelect)

  def timeMinNArray(reps:Int) = run(reps)(minNArray)
  def minNArray = get(array)
}

object InPlaceMinN extends Serializable {

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
            val minN = findNthDoubleInPlace(ArrayView(rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)).toArray), n)
//              val minN = quickSelectDouble(rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)), n)
            data.setDouble(col, row, minN)
          }else { // integer values
          val minN = findNthIntInPlace(ArrayView(rs.map(r => r.get(col,row)).filter(num => !isNoData(num)).toArray), n)
//            val minN = quickSelectInt(rs.map(r => r.get(col,row)).filter(num => !isNoData(num)), n)
            data.set(col, row, minN)
          }
        }
      }
      ArrayRaster(data,re)
    }
  }
}

object ArrayMinN extends Serializable {

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
            val sorted = rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)).toArray.sorted
            val minN = {
              if(n < sorted.length) { sorted(n) }
              else { Double.NaN }
            }
            data.setDouble(col, row, minN)
          }else { // integer values
            val sorted = rs.map(r => r.get(col,row)).filter(num => !isNoData(num)).toArray.sorted
            val minN = {
              if(n < sorted.length) { sorted(n) }
              else { NODATA }
            }
            data.set(col, row, minN)
          }
        }
      }
      ArrayRaster(data,re)
    }
  }
}