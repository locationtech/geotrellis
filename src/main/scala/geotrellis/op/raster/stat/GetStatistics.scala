package geotrellis.op.raster.stat

import geotrellis._
import geotrellis.op._

import geotrellis.process.Result
import geotrellis.stat.{Histogram => HistogramObj}


/** 
 * Determine statistical data for the given histogram.
 *
 * This includes mean, median, mode, stddev, and min and max values.
 */
case class GetStatistics(h:Op[HistogramObj]) extends Op1(h) ({
  (h) => Result(h.generateStatistics)
})
