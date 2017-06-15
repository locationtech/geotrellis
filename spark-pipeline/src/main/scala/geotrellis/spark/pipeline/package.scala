package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.spark.pipeline.json._

import org.apache.spark.rdd.RDD

import scala.util.Try

package object pipeline {
  type PipelineConstructor = List[PipelineExpr]
  type LabeledListRDD = List[(String, RDD[Any])]
  type ListRDD = List[RDD[Any]]

  implicit class withGetCRS[T <: { def crs: String }](o: T) {
    def getCRS = Try(CRS.fromName(o.crs)) getOrElse CRS.fromString(o.crs)
  }

  implicit class withGetOptionCRS[T <: { def crs: Option[String] }](o: T) {
    def getCRS = o.crs.map(c => Try(CRS.fromName(c)) getOrElse CRS.fromString(c))
  }
}
