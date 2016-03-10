package geotrellis.spark.io.index.hilbert

import org.scalatest._
import org.joda.time.DateTime
import scala.collection.immutable.TreeSet
import geotrellis.spark.GridTimeKey
import geotrellis.spark.KeyBounds

class HilbertGridTimeKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 16 // corresponds to width of 4 2^4
  val y2k = new DateTime(2000,1,1,0,0,0,0)
  describe("HilbertGridTimeKeyIndex tests"){

    it("indexes col, row, and time"){
      val hst = HilbertGridTimeKeyIndex(GridTimeKey(0,0,y2k), GridTimeKey(0,0,y2k.plusMillis(upperBound)),4 , 4)
      val keys =
        for(col <- 0 until upperBound;
             row <- 0 until upperBound;
                t <- 0 until upperBound) yield {
          hst.toIndex(GridTimeKey(col,row,y2k.plusMillis(t)))
        }

      keys.distinct.size should be (upperBound * upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound * upperBound - 1)
    }


    it("generates hand indexes you can hand check 3x3x2"){
     val hilbert = HilbertGridTimeKeyIndex(GridTimeKey(0,0,y2k), GridTimeKey(2,2,y2k.plusMillis(1)),2,1)
     val idx = List[GridTimeKey](GridTimeKey(0,0,y2k), GridTimeKey(0,1,y2k),
                                  GridTimeKey(1,1,y2k), GridTimeKey(1,0,y2k),
                                  GridTimeKey(1,0,y2k.plusMillis(1)), GridTimeKey(1,1,y2k.plusMillis(1)),
                                  GridTimeKey(0,1,y2k.plusMillis(1)), GridTimeKey(0,0,y2k.plusMillis(1)))

     for(i<-0 to 7 ){
       hilbert.toIndex(idx(i)) should be (i)
     }
    }

    it("Generates a Seq[(Long,Long)] given a key range (GridKey,GridKey)"){
      // See http://mathworld.wolfram.com/HilbertCurve.html for reference
      val t1 = y2k
      val t2 = y2k.plusMillis(1)

      //hand checked examples for a 2x2x2
      val hilbert = HilbertGridTimeKeyIndex(GridTimeKey(0,0,t1), GridTimeKey(1,1,t2), 1, 1)

      // select origin point only
      var idx = hilbert.indexRanges((GridTimeKey(0,0,t1), GridTimeKey(0,0,t1)))
      idx.length should be (1)
      idx.toSet should be (Set(0->0))

      // select the whole space
      // 2x2x2 space has 8 cells, we should cover it in one range: (0, 7)
      idx = hilbert.indexRanges((GridTimeKey(0,0,t1), GridTimeKey(1,1,t2)))
      idx.length should be (1)
      idx.toSet should be (Set(0->7))

      // first 4 sub cubes (along y), front face
      idx = hilbert.indexRanges((GridTimeKey(0,0,t1), GridTimeKey(1,1,t1)))
      idx.length should be (3)
      idx.toSet should be (Set(0->0, 3->4, 7->7))

      // second 4 sub cubes (along y), back face
      idx = hilbert.indexRanges((GridTimeKey(0,0,t2), GridTimeKey(1,1,t2)))
      idx.length should be (2)
      idx.toSet should be (Set(1->2, 5->6))

      //4 sub cubes (along x), bottom face
      idx = hilbert.indexRanges((GridTimeKey(0,0,t1), GridTimeKey(1,0,t2)))
      idx.length should be (2)
      idx.toSet should be (Set(0->1, 6->7))

      //next 4 sub cubes (along x), top face
      idx = hilbert.indexRanges((GridTimeKey(0,1,t1), GridTimeKey(1,1,t2)))
      idx.length should be (1)
      idx.toSet should be (Set(2->5))
    }
  }
}
