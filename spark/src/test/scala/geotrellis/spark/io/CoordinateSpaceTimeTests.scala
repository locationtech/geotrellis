package geotrellis.spark.io

import com.github.nscala_time.time.Imports._
import geotrellis.raster.{GridBounds, Tile}
import geotrellis.spark._
import org.joda.time.DateTime

trait CoordinateSpaceTimeTests { self: PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData] =>
  val dates = Vector( // all the dates in the layer
    new DateTime(2010,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2011,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2012,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2013,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2014,1,1,0,0,0, DateTimeZone.UTC))
  val bounds1 = GridBounds(1,1,3,3)
  val bounds2 = GridBounds(4,5,6,6)

  it("query outside of layer bounds") {
    query.where(Intersects(GridBounds(10, 10, 15, 15))).toRDD.collect() should be(empty)
  }

  it("query disjunction on space") {
    val actual = query.where(Intersects(bounds1) or Intersects(bounds2)).toRDD.keys.collect()

    val expected = {
      for {
        (col, row) <- bounds1.coords ++ bounds2.coords
        time <- dates
      } yield SpaceTimeKey(col, row, time)
    }

    if (expected.diff(actual).nonEmpty)
      info(s"missing: ${(expected diff actual).toList}")
    if (actual.diff(expected).nonEmpty)
      info(s"unwanted: ${(actual diff expected).toList}")

    actual should contain theSameElementsAs expected
  }

  it("query disjunction on space and time") {
    val actual = query.where(Intersects(bounds1) or Intersects(bounds2))
      .where(Between(dates(0), dates(1)) or Between(dates(3), dates(4))).toRDD.keys.collect()

    val expected = {
      for {
        (col, row) <- bounds1.coords ++ bounds2.coords
        time <- dates diff Seq(dates(2))
      } yield {
        SpaceTimeKey(col, row, time)
      }
    }

    if (expected.diff(actual).nonEmpty)
      info(s"missing: ${(expected diff actual).toList}")
    if (actual.diff(expected).nonEmpty)
      info(s"unwanted: ${(actual diff expected).toList}")

    actual should contain theSameElementsAs expected
  }
}
