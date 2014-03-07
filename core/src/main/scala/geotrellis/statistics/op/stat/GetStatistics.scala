package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics.Histogram


/** 
 * Determine statistical data for the given histogram.
 *
 * This includes mean, median, mode, stddev, and min and max values.
 */
case class GetStatistics(h:Op[Histogram]) extends Op1(h) ({
  (h) => Result(h.generateStatistics)
})
