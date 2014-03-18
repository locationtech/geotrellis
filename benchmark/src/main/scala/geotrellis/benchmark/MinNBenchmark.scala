package geotrellis.benchmark

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object MinNImplementation extends BenchmarkRunner(classOf[MinNImplementation])
class MinNImplementation extends OperationBenchmark {
  @Param(Array("64", "512", "1024"))
  var size:Int = 0

  var quickSelectInPlace: Raster = null
  var quickSelectImmutable: Raster = null
  var arraySort: Raster = null

  override def setUp() {
    val r = loadRaster("SBN_farm_mkt", size, size)
    val r1 = (r+1)
    val r2 = (r+2)
    val r3 = (r+3)
    val r4 = (r+4)
    val r5 = (r+5)

    quickSelectInPlace = MinN(2, r1, r2, r3, r4, r5)
    quickSelectImmutable = ImmutableMinN(2, r1, r2, r3, r4, r5)
    arraySort = ArrayMinN(2, r1, r2, r3, r4, r5)
  }

  def timeMinNQuickSelectInPlace(reps:Int) = run(reps)(minNInPlace)
  def minNInPlace = get(quickSelectInPlace)

  def timeMinNQuickSelectImmutable(reps:Int) = run(reps)(minNImmutable)
  def minNImmutable = get(quickSelectImmutable)

  def timeMinNArray(reps:Int) = run(reps)(minNArray)
  def minNArray = get(arraySort)
}

object ImmutableMinN extends Serializable {

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
            val minN = quickSelectDouble(rs.map(r => r.getDouble(col,row)).filter(num => !isNoData(num)), n)
            data.setDouble(col, row, minN)
          }else { // integer values
            val minN = quickSelectInt(rs.map(r => r.get(col,row)).filter(num => !isNoData(num)), n)
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