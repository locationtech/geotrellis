package geotrellis.vectortile

import org.scalatest._

import geotrellis.vector._

class VectorTileTest extends FunSuite {

    // Since the encoder has not been written yet, this will have very few
    // tests for not.

    test("Real Data 1") {
        /* This still fails to make a Polygon with the following points:
           List((257.25,21.1875),
                (258.0,21.0625),
                (258.0,25.5625),
                (258.0,25.375),
                (258.0,23.3125),
                (257.5625,23.375),
                (257.25,21.1875))
           This is really weird looking... staying on the 258.0 x-coordinate a
           bunch so it makes sense that it fails... but it's currently hard
           for me to check that's what was intended to be coded. I'll also
           check that the source is using the most recent version of the
           specification in case that's the problem.
        */
        val vt = new VectorTile("data/test.mvt")
        // TODO check that the values are accurate
    }

    test("Real Data 2") {
        val vt = new VectorTile("data/test2.mvt")
        // TODO check that the values are accurate
    }

}

