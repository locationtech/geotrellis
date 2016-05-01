package geotrellis.vectortile

import org.scalatest._

import geotrellis.vector._

class VectorTileTest extends FunSuite {

    // Test cases generated from java-vector-tile
    // https://github.com/ElectronicChartCentre/java-vector-tile
    // some of the source code used to generate the test files can be found in
    // scala-vector-tile/data/DataGen/

    test("SingleLayer") {
        val vt = new VectorTile("data/SingleLayer.mvt")
        assert(vt.layers.size == 1)
        val geometries = vt.geometries()
        assert(geometries.size == 1)
        val expected = Line(Seq[Point](
            Point(3, 6),
            Point(8, 12),
            Point(20, 34)
        ))
        assert(geometries contains expected)
    }

    test("MultiLayer") {
        val vt = new VectorTile("data/MultiLayer.mvt")
        assert(vt.layers.size == 2)
        val geometries1 = vt.geometriesByName(Set[String]("DEPCNT"))
        val geometries2 = vt.geometriesByName(Set[String]("TNCPED"))
        assert(geometries1.size == 1)
        assert(geometries2.size == 1)
        val expected1 = Line(Seq[Point](
            Point(3, 6),
            Point(8, 12),
            Point(20, 34)
        ))
        val expected2 = Line(Seq[Point](
            Point(3, 6),
            Point(8, 12),
            Point(20, 34),
            Point(33, 72)
        ))
        assert(geometries1 contains expected1)
        assert(geometries2 contains expected2)
    }

    test("GrabBag") {
        // a bunch of differen types of geometries
        val vt = new VectorTile("data/GrabBag.mvt")
        // just a sanity check, the command tests are sufficient.
    }

    test("PolygonWithHole") {
        // https://github.com/mapbox/vector-tile-spec/tree/master/2.1
        // right before section 4 there's an example of ``negative space''
        // the current build does not deal with this and instead makes each
        // polygon separate. when this is decided upon, a test should be added
        // to CommandTest.scala

        // options: comp geo to figure it out, additional command that
        // indicates that the next polygon is a hole, etc
        val vt = new VectorTile("data/PolygonWithHole.mvt")
        assert(false)
    }

    test("Tagged") {
        val vt = new VectorTile("data/Tagged.mvt")
        val expected = Point(3, 6)
        var geometries = vt.filteredGeometries(
            (layer, feature) => feature.tags("key1").value == "value1"
        )
        assert(geometries.size == 1)
        assert(geometries contains expected)
        geometries = vt.filteredGeometries(
            (layer, feature) => feature.tags("key2").value == 123
        )
        assert(geometries.size == 1)
        assert(geometries contains expected)
        geometries = vt.filteredGeometries(
            (layer, feature) => feature.tags("key3").value == 234.1f
        )
        assert(geometries.size == 1)
        assert(geometries contains expected)
        geometries = vt.filteredGeometries(
            (layer, feature) => feature.tags("key4").value == 567.123d
        )
        assert(geometries.size == 1)
        assert(geometries contains expected)
        geometries = vt.filteredGeometries(
            (layer, feature) => feature.tags("key5").value == -123
        )
        assert(geometries.size == 1)
        assert(geometries contains expected)
        geometries = vt.filteredGeometries(
            (layer, feature) => feature.tags("key6").value == "value6"
        )
        assert(geometries.size == 1)
        assert(geometries contains expected)
        geometries = vt.filteredGeometries(
            (layer, feature) => (feature.tags get "unknownkey") != None
        )
        assert(geometries.size == 0)
    }

    test("BigTile Data") {
        // https://github.com/ElectronicChartCentre/java-vector-tile/blob/master/src/test/resources/bigtile.vector.pbf
        val vt = new VectorTile("data/BigTile.mvt")
        // just a sanity check that I can read data I didn't write
    }

}

