package geotrellis.spark.tiling

import geotrellis.raster.resample._

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner.defaultPartitioner

object Tiler {
  case class Options(
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None
  )

  object Options {
    def DEFAULT = Options()

    implicit def methodToOptions(method: ResampleMethod): Options =
      Options(resampleMethod = method)
  }
}
