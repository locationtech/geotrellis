package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.index._

trait SpatialKeyIndexMethods {
  def keyIndexMethods: Map[String, KeyIndexMethod[SpatialKey]] =
    Map(
      "row major" -> RowMajorKeyIndexMethod,
      "z order" -> ZCurveKeyIndexMethod,
      "hilbert" -> HilbertKeyIndexMethod
    )
}
