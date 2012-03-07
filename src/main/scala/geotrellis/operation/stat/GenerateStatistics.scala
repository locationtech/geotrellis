package geotrellis.operation

import geotrellis.process._
import geotrellis.stat._

/** 
 * Determine statistical data for the given histogram.
 *
 * This includes mean, median, mode, stddev, and min and max values.
 */
case class GenerateStatistics(h:Op[Histogram]) extends Op1(h) ({
  (h) => Result(h.generateStatistics)
})
