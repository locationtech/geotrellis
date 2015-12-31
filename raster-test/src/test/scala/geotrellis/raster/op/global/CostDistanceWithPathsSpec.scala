package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.raster.op.global._

import geotrellis.vector.Line

import geotrellis.testkit._

import org.scalatest._

class CostDistanceWithPathsSpec extends FunSpec with Matchers {

  val Eps = 1e-7

  describe("Cost Distance with remembering optimal paths") {

    it("should find single best path #1") {
      val tile = ArrayTile(Array(
        1,  10, 10, 10, 10,
        10, 1,  10, 10, 10,
        10, 10, 1,  10, 10,
        10, 10, 10, 1,  10,
        10, 10, 10, 10,  1
      ), 5, 5)

      val correctCost = math.sqrt(2) * 4
      val correctPath = Line(List(
        (0.0, 0.0),
        (1.0, 1.0),
        (2.0, 2.0),
        (3.0, 3.0),
        (4.0, 4.0)
      ))

      val res = tile.costDistanceWithPaths((0, 0))
      val (cost, paths) = res.getPath((4, 4))

      cost should be (correctCost +- Eps)
      paths.size should be (1)

      val path = paths.head
      path should be(correctPath)
    }

    it("should find single best path #2") {
      val tile = ArrayTile(Array(
        1,   1,  1,  1, 10,
        10, 10, 10, 10, 1,
        10, 10, 10, 10, 1,
        10, 10, 10, 10, 1,
        10, 10, 10, 10, 1
      ), 5, 5)

      val correctCost = math.sqrt(2) + 6
      val correctPath = Line(List(
        (0.0, 0.0),
        (1.0, 0.0),
        (2.0, 0.0),
        (3.0, 0.0),
        (4.0, 1.0),
        (4.0, 2.0),
        (4.0, 3.0),
        (4.0, 4.0)
      ))

      val res = tile.costDistanceWithPaths((0, 0))
      val (cost, paths) = res.getPath((4, 4))

      cost should be (correctCost +- Eps)
      paths.size should be (1)

      val path = paths.head
      path should be(correctPath)
    }

    it("should find both best paths #1") {
      val tile = ArrayTile(Array(
        1,  10, 10, 10, 10,
        10,  1, 10,  1, 10,
        10, 10,  1, 10,  1,
        10,  1, 10, 10,  1,
        10, 10,  1,  1,  1
      ), 5, 5)

      val correctCost = math.sqrt(2) * 4 + 2
      val correctPaths = Set(
        Line(List(
          (0.0, 0.0), (1.0, 1.0), (2.0, 2.0),
          (3.0, 1.0), (4.0, 2.0), (4.0, 3.0),
          (4.0, 4.0)
        )),
          Line(List(
            (0.0, 0.0), (1.0, 1.0), (2.0, 2.0),
            (1.0, 3.0), (2.0, 4.0), (3.0, 4.0),
            (4.0, 4.0)
          ))
      )

      val res = tile.costDistanceWithPaths((0, 0))
      val (cost, paths) = res.getPath((4, 4))

      cost should be (correctCost +- Eps)
      paths.size should be (2)

      paths.toSet should be(correctPaths)
    }

  }

  it("should find both best paths #2") {
    val tile = ArrayTile(Array(
       1,  1,  1, 10, 10,
       1, 10, 10,  1, 10,
       1, 10,  1, 10, 10,
      10,  1, 10,  1, 10,
      10, 10, 10, 10,  1
    ), 5, 5)

    val correctCost = math.sqrt(2) * 4 + 2
    val correctPaths = Set(
      Line(List(
        (0.0, 0.0), (1.0, 0.0), (2.0, 0.0),
        (3.0, 1.0), (2.0, 2.0), (3.0, 3.0),
        (4.0, 4.0)
      )),
      Line(List(
        (0.0, 0.0), (0.0, 1.0), (0.0, 2.0),
        (1.0, 3.0), (2.0, 2.0), (3.0, 3.0),
        (4.0, 4.0)
      ))
    )

    val res = tile.costDistanceWithPaths((0, 0))
    val (cost, paths) = res.getPath((4, 4))

    cost should be (correctCost +- Eps)
    paths.size should be (2)

    paths.toSet should be(correctPaths)
  }

}
