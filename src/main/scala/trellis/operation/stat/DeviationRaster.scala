package trellis.operation

import trellis.raster.IntRaster
import trellis.stat.{Histogram, Statistics}
import trellis.process.{Server,Results}

/*
 * Calculate a raster in which each value is set to the standard deviation of that cell's value.
 */
case class DeviationRaster(r:IntRasterOperation, h:Operation[Histogram],
                           factor:Int) extends IntRasterOperation {
  def childOperations = { List(r, h) }
  def _run(server:Server, cb:Callback) = {
    val g = GenerateStatistics(h)
    runAsync(List(g,r),server,cb)
  }

  val nextSteps:Steps = {
    case Results(List(g:Statistics, r:IntRaster)) => step2(g,r)
  }

  def step2 (stats:Statistics, raster:IntRaster) = {
    val indata = raster.data
    val len = indata.length
    val outdata = Array.ofDim[Int](len)

    val mean = stats.mean
    val stddev = stats.stddev

    var i = 0
    while (i < len) {
      val delta = indata(i) - mean
      outdata(i) = (delta * factor / stddev).asInstanceOf[Int]
      i += 1
    }
    val output = IntRaster(outdata, raster.rows, raster.cols, raster.rasterExtent.copy)
    Some(output)
  }
}
