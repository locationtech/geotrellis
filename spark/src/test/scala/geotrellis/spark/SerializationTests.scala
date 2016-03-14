package geotrellis.spark

import geotrellis.proj4._
import geotrellis.raster.io.geotiff._
import geotrellis.spark.testkit._
import geotrellis.vector._

import org.apache.hadoop.fs.Path
import org.scalatest._

class SerializationTests extends FunSuite with Matchers {
  test("Serializing CRS's") {
    val crs = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")
    assert(crs == LatLng)

    {
      val t = Transform(crs, LatLng)
      val expected = (141.7066666666667, -17.946666666666676)
      val actual = t(expected._1, expected._2)
      assert(actual == expected)
    }

    {
      val (crs1, crs2) = (crs.serializeAndDeserialize, LatLng.serializeAndDeserialize)
      assert(crs1 == crs2)
      val t = Transform(crs1, crs2)
      val expected = (141.7066666666667, -17.946666666666676)
      val actual = t(expected._1, expected._2)
      actual should be (expected)
    }

    {
      val t = Transform(LatLng, crs)
      val expected = (141.7154166666667,-17.52875000000001)
      val actual = t(expected._1, expected._2)
      assert(actual == expected)
    }

    {
      val t = Transform(LatLng, crs.serializeAndDeserialize)
      val expected = (141.7154166666667,-17.52875000000001)
      val actual = t(expected._1, expected._2)
      assert(actual == expected)
    }

  }
}
