package geotrellis.spark

import _root_.io.circe.Json
import cats.Monoid
import geotrellis.proj4.CRS
import geotrellis.spark.pipeline.json.PipelineExpr
import org.apache.spark.rdd.RDD

import scala.util.Try

package object pipeline extends json.Implicits {
  type PipelineConstructor = List[PipelineExpr]
  type LabeledListRDD = List[(String, RDD[Any])]
  type ListRDD = List[RDD[Any]]

  implicit class withGetCRS[T <: { def crs: String }](o: T) {
    def getCRS = Try(CRS.fromName(o.crs)) getOrElse CRS.fromString(o.crs)
  }

  implicit class withGetOptionCRS[T <: { def crs: Option[String] }](o: T) {
    def getCRS = o.crs.map(c => Try(CRS.fromName(c)) getOrElse CRS.fromString(c))
  }

  implicit val jsonDeepMergeMonoid: Monoid[Json] = new Monoid[Json] {
    val empty = Json.Null
    def combine(x: Json, y: Json): Json = x.deepMerge(y)
  }
}
