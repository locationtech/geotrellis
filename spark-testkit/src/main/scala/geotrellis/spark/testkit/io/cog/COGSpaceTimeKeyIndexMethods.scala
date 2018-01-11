package geotrellis.spark.testkit.io.cog

import geotrellis.spark._
import geotrellis.spark.io.index._

import jp.ne.opt.chronoscala.Imports._

import java.time.ZonedDateTime

trait COGSpaceTimeKeyIndexMethods {
  def keyIndexMethods: Map[String, KeyIndexMethod[SpaceTimeKey]] =
    Map(
      "z order by year" -> ZCurveKeyIndexMethod.byYear,
      "z order by 6 months" -> ZCurveKeyIndexMethod.byMonths(6),
      "hilbert using now" -> HilbertKeyIndexMethod(ZonedDateTime.now - 20.years, ZonedDateTime.now, 10),
      "hilbert resolution" -> HilbertKeyIndexMethod(10)
    )
}
