package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.index._

import com.github.nscala_time.time.Imports._

trait SpaceTimeKeyIndexMethods {
  def keyIndexMethods: Map[String, KeyIndexMethod[SpaceTimeKey]] =
    Map(
      "z order by year" -> ZCurveKeyIndexMethod.byYear,
      "z order by 6 months" -> ZCurveKeyIndexMethod.byMonths(6),
      "hilbert using now" -> HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4),
      "hilbert resolution" -> HilbertKeyIndexMethod(2)
    )
}
