package geotrellis.spark.io.index.hilbert

import org.scalatest._
import org.joda.time.DateTime
import scala.collection.immutable.TreeSet
import geotrellis.spark.SpaceTimeKey
import geotrellis.spark.KeyBounds

class HilbertSpaceTimeKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 16
  val y2k = new DateTime(2000,1,1,0,0,0,0)
  describe("HilbertSpaceTimeKeyIndex tests"){

    it("indexes col, row, and time"){
      val hst = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,0,y2k.plusMillis(5)),5 , 5)
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

     //hand checked examples for a 2x2x2
     val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,0,y2k.plusMillis(2)), 2, 2)

     //2x2x2 cube
     var idx = hilbert.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(2))))
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (8)

     //first 4 sub cubes (along y)
     idx = hilbert.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,2,y2k.plusMillis(2))))
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (4)

     //second 4 sub cubes (along y)
     idx = hilbert.indexRanges((SpaceTimeKey(1,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(2))))
     idx.length should be (1)
     idx(0)._1 should be (4)
     idx(0)._2 should be (8)

     //4 sub cubes (along x)
     idx = hilbert.indexRanges((SpaceTimeKey(0,1,y2k), SpaceTimeKey(2,2,y2k.plusMillis(2))))
     idx.length should be (1)
     idx(0)._1 should be (2)
     idx(0)._2 should be (6)

     //next 4 sub cubes (along x)
     idx = hilbert.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,1,y2k.plusMillis(2))))
     idx.length should be (2)
     idx(0)._1 should be (0)
     idx(0)._2 should be (2)
     idx(1)._1 should be (6)
     idx(1)._2 should be (8)

     //single point
     idx = hilbert.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (1)
    }
  }
}
