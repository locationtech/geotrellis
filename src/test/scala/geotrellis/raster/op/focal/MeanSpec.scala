package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class MeanSpec extends FunSpec with FocalOpSpec
                               with ShouldMatchers {

  val getCursorMeanResult = (getDoubleCursorResult _).curried((r,n) => Mean(r,n))(Circle(1))
  val getCellwiseMeanResult = Function.uncurried((getDoubleCellwiseResult _).curried((r,n) => Mean(r,n))(Square(1)))

  describe("Mean") {
    it("should handle all NODATA") {
      getCursorMeanResult(MockCursor.fromAll(NODATA,NODATA,NODATA,NODATA)).isNaN should be (true)
    }

    it("should match histogram mean default set in cursor calculation") {      
      for(s <- defaultTestSets) {
        val sf = s.filter { x => x != NODATA }
        val expected = sf.sum / sf.length.toDouble
        if(expected.isNaN) {
          getCursorMeanResult(MockCursor.fromAddRemove(s,Seq[Int]())).isNaN should equal (true)
        } else {
          getCursorMeanResult(MockCursor.fromAddRemove(s,Seq[Int]())) should equal (expected)
        }
      }
    }

    it("should match histogram mean default set in cellwise calculation") {      
      for(s <- defaultTestSets) {
        val sf = s.filter { x => x != NODATA }
        val expected = sf.sum / sf.length.toDouble
        if(expected.isNaN) {
          getCellwiseMeanResult(s,Seq[Int]()).isNaN should equal (true)
        } else {
          getCellwiseMeanResult(s,Seq[Int]()) should equal (expected)
        }
      }
    }

    it("should hold state correctly with cursor calculation") {
      testDoubleCursorSequence((r,n) => Mean(r,n), Circle(1),
             Seq( SeqTestSetup(Seq(1,2,3,4,5), Seq[Int](), 3.0),
                  SeqTestSetup(Seq(10,10)    , Seq(2,3,5), 6.25)) )
    }

    it("should hold state correctly with cellwise calculation") {
      testDoubleCellwiseSequence((r,n)=>Mean(r,n), Square(1),
             Seq( SeqTestSetup(Seq(1,2,3,4,5), Seq[Int](), 3.0),
                  SeqTestSetup(Seq(10,10)    , Seq(2,3,5), 6.25)) )
    }
  }
}
