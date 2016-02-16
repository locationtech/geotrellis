package geotrellis.raster

import geotrellis.raster.testkit._

import org.scalatest._

class MultibandCombinersSpec extends FunSuite with RasterMatchers with Matchers {
  val fillNumber = 99
  val tile       = IntConstantTile(fillNumber, 2, 2)

  test("Multiband combine function test: arity 2") {
    val arity = 2
    val combined = ArrayMultiBandTile(tile, tile).combine(0, 1) { case (b0, b1) => b0 + b1 }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 3") {
    val arity = 3
    val combined = ArrayMultiBandTile(tile, tile, tile).combine(0, 1, 2) { case (b0, b1, b2) => b0 + b1 + b2 }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 4") {
    val arity = 4
    val combined = ArrayMultiBandTile(tile, tile, tile, tile).combine(0, 1, 2, 3) {
      case (b0, b1, b2, b3) => b0 + b1 + b2 + b3
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 5") {
    val arity = 5
    val combined = ArrayMultiBandTile(tile, tile, tile, tile, tile).combine(0, 1, 2, 3, 4) {
      case (b0, b1, b2, b3, b4) => b0 + b1 + b2 + b3 + b4
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 6") {
    val arity = 6
    val combined = ArrayMultiBandTile(tile, tile, tile, tile, tile, tile).combine(0, 1, 2, 3, 4, 5) {
      case (b0, b1, b2, b3, b4, b5) => b0 + b1 + b2 + b3 + b4 + b5
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 7") {
    val arity = 7
    val combined = ArrayMultiBandTile(tile, tile, tile, tile, tile, tile, tile).combine(0, 1, 2, 3, 4, 5, 6) {
      case (b0, b1, b2, b3, b4, b5, b6) => b0 + b1 + b2 + b3 + b4 + b5 + b6
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 8") {
    val arity = 8
    val combined = ArrayMultiBandTile(tile, tile, tile, tile, tile, tile, tile, tile).combine(0, 1, 2, 3, 4, 5, 6, 7) {
      case (b0, b1, b2, b3, b4, b5, b6, b7) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 9") {
    val arity = 9
    val combined = ArrayMultiBandTile(tile, tile, tile, tile, tile, tile, tile, tile, tile).combine(0, 1, 2, 3, 4, 5, 6, 7, 8) {
      case (b0, b1, b2, b3, b4, b5, b6, b7, b8) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 10") {
    val arity = 10
    val combined = ArrayMultiBandTile(tile, tile, tile, tile, tile, tile, tile, tile, tile, tile).combine(0, 1, 2, 3, 4, 5, 6, 7, 8, 9) {
      case (b0, b1, b2, b3, b4, b5, b6, b7, b8, b9) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9
    }
    val expected = IntConstantTile(fillNumber * arity, 2, 2)

    assert(combined.toArray === expected.toArray)
  }
}
