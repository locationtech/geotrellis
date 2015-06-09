package geotrellis.spark.io.index.hilbert

import org.scalatest._
import org.joda.time.DateTime
import scala.collection.immutable.TreeSet
import geotrellis.spark.SpaceTimeKey
import geotrellis.spark.KeyBounds

class HilbertSpaceTimeKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 16 // corresponds to width of 4 2^4
  val y2k = new DateTime(2000,1,1,0,0,0,0)
  describe("HilbertSpaceTimeKeyIndex tests"){

    it("indexes col, row, and time"){
      val hst = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,0,y2k.plusMillis(upperBound)),4 , 4)
      val keys = 
        for(col <- 0 until upperBound;
             row <- 0 until upperBound;
                t <- 0 until upperBound) yield {
          hst.toIndex(SpaceTimeKey(col,row,y2k.plusMillis(t)))
        } 

      keys.distinct.size should be (upperBound * upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound * upperBound - 1)
    }
     

    it("generates hand indexes you can hand check 2x2x2"){
     val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(1)),1,1) 
     val idx = List[SpaceTimeKey](SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,1,y2k),
                                  SpaceTimeKey(1,1,y2k), SpaceTimeKey(1,0,y2k),
                                  SpaceTimeKey(1,0,y2k.plusMillis(1)), SpaceTimeKey(1,1,y2k.plusMillis(1)),
                                  SpaceTimeKey(0,1,y2k.plusMillis(1)), SpaceTimeKey(0,0,y2k.plusMillis(1)))
     
     for(i<-0 to 7 ){
       hilbert.toIndex(idx(i)) should be (i)
     }
    }

    it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){
      val t1 = y2k
      val t2 = y2k.plusMillis(1)

      //hand checked examples for a 2x2x2 
      // note: The col/row of the end key does not matter here, eh ?
      val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,t1), SpaceTimeKey(1,1,t2), 1, 1)

      // select origin point only
      var idx = hilbert.indexRanges((SpaceTimeKey(0,0,t1), SpaceTimeKey(0,0,t1)))
      idx.length should be (1)
      idx.toSet should be (Set(0->0))

      // select the whole space
      // 2x2x2 space has 8 cells, we should cover it in one range: (0, 7)
      idx = hilbert.indexRanges((SpaceTimeKey(0,0,t1), SpaceTimeKey(1,1,t2)))
      idx.length should be (1)
      idx.toSet should be (Set(0->7))

      // first 4 sub cubes (along y), front face
      idx = hilbert.indexRanges((SpaceTimeKey(0,0,t1), SpaceTimeKey(1,1,t1)))
      idx.length should be (1)
      idx.toSet should be (Set(0->3))

      // second 4 sub cubes (along y), back face
      idx = hilbert.indexRanges((SpaceTimeKey(0,0,t2), SpaceTimeKey(1,1,t2)))
      idx.length should be (1)
      idx.toSet should be (Set(4->7))

      //4 sub cubes (along x), floor
      idx = hilbert.indexRanges((SpaceTimeKey(0,0,t1), SpaceTimeKey(1,0,t2)))
      idx.length should be (3)
      idx.toSet should be (Set(0->0, 3->4, 7->7))

      //next 4 sub cubes (along x)
      idx = hilbert.indexRanges((SpaceTimeKey(0,1,t1), SpaceTimeKey(1,1,t2)))
      idx.length should be (2)
      idx.toSet should be (Set(1->2, 5->6))
    } 
  }
}
