package geotrellis.vectortile

import org.scalatest._

class SanityTest extends FunSuite {

    test("simple test") {
        val vt = new VectorTile("data/test.mvt")
        println(vt)
    }

}

