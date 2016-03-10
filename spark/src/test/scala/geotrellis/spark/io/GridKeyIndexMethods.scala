package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.index._

trait GridKeyIndexMethods {
  def keyIndexMethods: Map[String, KeyIndexMethod[GridKey]] =
    Map(
      "row major" -> RowMajorKeyIndexMethod,
      "z order" -> ZCurveKeyIndexMethod,
      "hilbert" -> HilbertKeyIndexMethod
    )
}
