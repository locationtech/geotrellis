package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.io.{RDDQuery, Intersects}
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.testfiles._

import org.scalatest._

class RDDQuerySpec extends FunSpec
  with TestEnvironment with TestFiles  with Matchers with OnlyIfCanRunSpark {

  ifCanRunSpark {
    describe("RasterQuerySpec") {
      val md = RasterMetaData(
        TypeFloat,
        LayoutDefinition(LatLng.worldExtent, TileLayout(8, 8, 3, 4)),
        Extent(-135.00000125, -89.99999, 134.99999125, 67.49999249999999),
        LatLng
        )

      val keyBounds = KeyBounds(SpatialKey(1, 1), SpatialKey(6, 7))

      it("should be better then Java serialization") {
        val query = new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(GridBounds(2, 2, 2, 2)))
        val outKeyBounds = query(md, keyBounds)
        info(outKeyBounds.toString)
      }

      it("should throw on intersecting regions") {
        val query = new RDDQuery[SpatialKey, RasterMetaData]
          .where(Intersects(GridBounds(2, 2, 2, 2)) or Intersects(GridBounds(2, 2, 2, 2)))

        intercept[RuntimeException] {
          query(md, keyBounds)
        }
      }

    }

    describe("RDDQuery KeyBounds generation") {
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
        val query = new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(bounds1))
        val expected = for ((x, y) <- bounds1.coords) yield SpatialKey(x, y)

        val found = query(md, kb).flatMap(spatialKeyBoundsKeys)
        info(s"missing: ${(expected diff found).toList}")
        info(s"unwanted: ${(found diff expected).toList}")

        found should contain theSameElementsAs expected
      }

      it("should generate KeyBounds for two regions") {
        val bounds1 = GridBounds(1, 1, 3, 3)
        val bounds2 = GridBounds(4, 5, 6, 6)
        val query = new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(bounds1) or Intersects(bounds2))
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