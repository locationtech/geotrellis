package trellis.operation

import trellis.stat.{Histogram, Statistics}
import trellis.process.Server

/** 
  * Determine mean, median, mode, stddev, and min and max values for a given raster. 
  */
case class GenerateStatistics(h:Operation[Histogram]) extends Operation[Statistics] with SimpleOperation[Statistics] {
  def childOperations = { List(h) }
  def _value(server:Server) = server.run(h).generateStatistics
}
