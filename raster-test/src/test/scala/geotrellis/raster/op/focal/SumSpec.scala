package geotrellis.raster.op.focal

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._

class SumSpec extends FunSpec with TestEngine with TileBuilders with FocalOpSpec {
  val sq1 = Square(1)
  val sq2 = Square(2)
  val sq3 = Square(3)

  val e = Extent(0.0, 0.0, 4.0, 4.0)
  val re = RasterExtent(e, 1.0, 1.0, 4, 4)

  val r = IntConstantTile(1, 4, 4)
  val rd = DoubleConstantTile(1.1, 4, 4)

  val data16 = Array(16, 16, 16, 16,
    16, 16, 16, 16,
    16, 16, 16, 16,
    16, 16, 16, 16)

  val getCursorSumResult = (getCursorResult _).curried{
    (r,n) => Sum.calculation(r,n)
  }(Circle(1))

  val getCellwiseSumResult = Function.uncurried(
    (getCellwiseResult _).curried(
      (r,n) => Sum.calculation(r,n)
    )(Square(1))
  )

  describe("Sum") {
    it("should match sum against default sets in cursor calculation") {
      for(added <- defaultTestSets) {
        for(removed <- defaultTestSets) {
          val filteredA = added.filter { x => isData(x) }
          val filteredR = removed.filter { x => isData(x) }
          val expected = filteredA.sum - filteredR.sum
          getCursorSumResult(MockCursor.fromAddRemove(added,removed)) should equal (expected)
        }
      }
    }

    it("should match sum against default sets in cellwise calculation") {
      for(added <- defaultTestSets) {
        for(removed <- defaultTestSets) {
          val filteredA = added.filter { x => isData(x) }
          val filteredR = removed.filter { x => isData(x) }
          val expected = filteredA.sum - filteredR.sum
          getCellwiseSumResult(added,removed) should equal (expected)
        }
      }
    }

    it("should hold state correctly with cursor calculation") {
      testCursorSequence((r,n) => Sum.calculation(r,n), Circle(1),
             Seq( SeqTestSetup(Seq(1,2,3,4,5), Seq[Int](), 15),
                  SeqTestSetup(Seq(10,10)    , Seq(2,3,5), 25)) )
    }

    it("should hold state correctly with cellwise calculation") {
      testCellwiseSequence((r,n)=>Sum.calculation(r,n), Square(1),
             Seq( SeqTestSetup(Seq(1,2,3,4,5), Seq[Int](), 15),
                  SeqTestSetup(Seq(10,10)    , Seq(2,3,5), 25)) )
    }

    it("should square sum r=1 for raster source") {
      val rs1 = createTile(Array(
         nd,1,1,      1,1,1,      1,1,1,
          1,1,1,      2,2,2,      1,1,1,

          1,1,1,      3,3,3,      1,1,1,
          1,1,1,     1,nd,1,      1,1,1
        ), 9, 4)

      val result = rs1.focalSum(Square(1))

      assertEqual(result, Array(
        3, 5, 7,    8, 9, 8,    7, 6, 4,
        5, 8,12,   15,18,15,   12, 9, 6,

        6, 9,12,   14,17,14,   12, 9, 6,
        4, 6, 8,    9,11, 9,    8, 6, 4))
    }



    it("should square sum r=1") {
      assertEqual(r.focalSum(Square(1)), Array(4, 6, 6, 4,
                                           6, 9, 9, 6,
                                           6, 9, 9, 6,
                                           4, 6, 6, 4))
    }

    it("should square sum r=1 double") {
      assertEqual(rd.focalSum(Square(1)), Array(4.4, 6.6, 6.6, 4.4,
                                            6.6, 9.9, 9.9, 6.6,
                                            6.6, 9.9, 9.9, 6.6,
                                            4.4, 6.6, 6.6, 4.4))
    }

    it("should square sum r=2") {
      assertEqual(r.focalSum(Square(2)), Array(9, 12, 12, 9,
                                           12, 16, 16, 12,
                                           12, 16, 16, 12,
                                           9, 12, 12, 9))
    }

    it("should square sum r=3+") {
      assertEqual(r.focalSum(Square(3)), data16)
      assertEqual(r.focalSum(Square(4)), data16)
      assertEqual(r.focalSum(Square(5)), data16)
    }

    it("should circle sum r=1") {
      assertEqual(r.focalSum(Circle(1)), Array(3, 4, 4, 3,
                                           4, 5, 5, 4,
                                           4, 5, 5, 4,
                                           3, 4, 4, 3))
    }

    it("should circle sum r=2") {
      assertEqual(r.focalSum(Circle(2)), Array(6, 8, 8, 6,
                                           8, 11, 11, 8,
                                           8, 11, 11, 8,
                                           6, 8, 8, 6))
    }

    it("should circle sum r=3") {
      assertEqual(r.focalSum(Circle(3)), Array(11, 13, 13, 11,
                                           13, 16, 16, 13,
                                           13, 16, 16, 13,
                                           11, 13, 13, 11))
    }

    it("should circle sum r=4+") {
      assertEqual(r.focalSum(Circle(4)), Array(15, 16, 16, 15,
                                           16, 16, 16, 16,
                                           16, 16, 16, 16,
                                           15, 16, 16, 15))
      assertEqual(r.focalSum(Circle(5)), data16)
      assertEqual(r.focalSum(Circle(6)), data16)
    }
  }
}
