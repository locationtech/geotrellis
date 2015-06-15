package geotrellis.spark

import com.github.nscala_time.time.Imports._
import geotrellis.raster._
import geotrellis.spark
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.testfiles._
import org.joda.time.DateTime

import org.scalatest._

class RasterRDDQuerySpec extends FunSpec
  with TestEnvironment with TestFiles  with Matchers with OnlyIfCanRunSpark {

  ifCanRunSpark {
    describe("RasterQuerySpec") {
      val md = RasterMetaData(
        TypeFloat,
        Extent(-135.00000125, -89.99999, 134.99999125, 67.49999249999999),
        LatLng,
        TileLayout(8, 8, 3, 4))

      val keyBounds = KeyBounds(SpatialKey(1, 1), SpatialKey(6, 7))

      it("should be better then Java serialization") {
        val query = new RasterRDDQuery[SpatialKey].where(Intersects(GridBounds(2, 2, 2, 2)))
        val outKeyBounds = query(md, keyBounds)
        info(outKeyBounds.toString)
      }

      it("should throw on intersecting regions") {
        val query = new RasterRDDQuery[SpatialKey]
          .where(Intersects(GridBounds(2, 2, 2, 2)) or Intersects(GridBounds(2, 2, 2, 2)))

        intercept[RuntimeException] {
          query(md, keyBounds)
        }
      }

    }

    describe("RasterRDDQuery KeyBounds generation") {
      def spatialKeyBoundsKeys(kb: KeyBounds[SpatialKey]) = {
        for {
          row <- kb.minKey.row to kb.maxKey.row
          col <- kb.minKey.col to kb.maxKey.col
        } yield SpatialKey(col, row)
      }


      val md = AllOnesTestFile.metaData
      val kb = KeyBounds[SpatialKey](SpatialKey(0, 0), SpatialKey(6, 7))

      it("should generate KeyBounds for single region") {
        val bounds1 = GridBounds(1, 1, 3, 2)
        val query = new RasterRDDQuery[SpatialKey].where(Intersects(bounds1))
        val expected = for ((x, y) <- bounds1.coords) yield SpatialKey(x, y)

        val found = query(md, kb).flatMap(spatialKeyBoundsKeys)
        info(s"missing: ${(expected diff found).toList}")
        info(s"unwanted: ${(found diff expected).toList}")

        found should contain theSameElementsAs expected
      }

      it("should generate KeyBounds for two regions") {
        val bounds1 = GridBounds(1, 1, 3, 3)
        val bounds2 = GridBounds(4, 5, 6, 6)
        val query = new RasterRDDQuery[SpatialKey].where(Intersects(bounds1) or Intersects(bounds2))
        val expected = for ((x, y) <- bounds1.coords ++ bounds2.coords) yield SpatialKey(x, y)

        val found = query(md, kb).flatMap(spatialKeyBoundsKeys)
        info(s"missing: ${(expected diff found).toList}")
        info(s"unwanted: ${(found diff expected).toList}")

        found should contain theSameElementsAs expected
      }
      // TODO: it would be nice to test SpaceTime too, but since time doesn't have a resolution we can not iterate
    }
  }
}

case class RasterRDDQueryTest[K](name: String, layerId: LayerId, query: RasterRDDQuery[K], expected: Seq[K])

object RasterRDDQueryTest {
  val spatialTest = List(
  {
    val bounds1 = GridBounds(1,1,3,3)

    RasterRDDQueryTest[SpatialKey](
      name = "query GridBounds",
      layerId = LayerId("ones", 10),
      query = new RasterRDDQuery[SpatialKey].where(Intersects(bounds1)),
      expected = for ( (x, y) <- bounds1.coords) yield SpatialKey(x,y))
  },
  {
    val bounds1 = GridBounds(1,1,3,3)
    val bounds2 = GridBounds(4,5,6,6)

    RasterRDDQueryTest[SpatialKey](
      name = "query disjoint GridBounds",
      layerId = LayerId("ones", 10),
      query = new RasterRDDQuery[SpatialKey].where(Intersects(bounds1) or Intersects(bounds2)),
      expected = for ( (x, y) <- bounds1.coords ++ bounds2.coords) yield SpatialKey(x,y))
  })

  val spatialTest_ones_ingested = List(
  {
    val bounds1 = GridBounds(915, 612, 916, 612)

    RasterRDDQueryTest[SpatialKey](
      name = "query GridBounds",
      layerId = LayerId("ones", 10),
      query = new RasterRDDQuery[SpatialKey].where(Intersects(bounds1)),
      expected = for ( (x, y) <- bounds1.coords) yield SpatialKey(x,y))
  },
  {
    val bounds1 = GridBounds(915, 612, 916, 612)
    val bounds2 = GridBounds(915, 613, 916, 613)

    RasterRDDQueryTest[SpatialKey](
      name = "query disjoint GridBounds",
      layerId = LayerId("ones", 10),
      query = new RasterRDDQuery[SpatialKey].where(Intersects(bounds1) or Intersects(bounds2)),
      expected = for ( (x, y) <- bounds1.coords ++ bounds2.coords) yield SpatialKey(x,y))
  })

  val spaceTimeTest = List(
  {
    val dates = Vector( // all the dates in the layer
      new DateTime(2010,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2011,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2013,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2014,1,1,0,0,0, DateTimeZone.UTC))
    val bounds1 = GridBounds(1,1,3,3)
    val bounds2 = GridBounds(4,5,6,6)

    RasterRDDQueryTest[SpaceTimeKey](
      name = "query Disjunction on space and time",
      layerId = LayerId("coordinates", 10),
      query = new RasterRDDQuery[SpaceTimeKey]
        .where(Intersects(bounds1) or Intersects(bounds2))
        .where(Between(dates(0), dates(1)) or Between(dates(2),dates(3))),
      expected = for {
        spatial <- bounds1.coords ++ bounds2.coords
        time <- dates
      } yield SpaceTimeKey(spatial._1, spatial._2, time)
    )


  },
  {
    val dates = List( // all the dates in the layer
      new DateTime(2010,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2011,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2012,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2013,1,1,0,0,0, DateTimeZone.UTC),
      new DateTime(2014,1,1,0,0,0, DateTimeZone.UTC))
    val bounds1 = GridBounds(1,1,3,3)
    val bounds2 = GridBounds(4,5,6,6)

    RasterRDDQueryTest[SpaceTimeKey](
      name = "query disjunction on space",
      layerId = LayerId("coordinates", 10),
      query = new RasterRDDQuery[SpaceTimeKey].where(Intersects(bounds1) or Intersects(bounds2)),
      expected = for {
        spatial <- bounds1.coords ++ bounds2.coords
        time <- dates
      } yield SpaceTimeKey(spatial._1, spatial._2, time)
    )
  })
}