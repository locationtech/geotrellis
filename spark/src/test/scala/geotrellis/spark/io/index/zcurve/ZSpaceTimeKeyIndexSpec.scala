package geotrellis.spark.io.index.zcurve

import org.scalatest._
import geotrellis.spark.SpaceTimeKey
import org.joda.time.DateTime
import scala.collection.immutable.TreeSet

class ZSpaceTimeKeySpec extends FunSpec with Matchers{
  val y2k = new DateTime(2000, 1, 1, 0, 0)
  val upperBound = 8
  val ymPattern = "YM"

  describe("ZSpaceTimeKey test"){

    it("indexes time"){
      val zst = new ZSpaceTimeKeyIndex({ dt => dt.getYear })

      val keys = 
        for(col <- 0 until upperBound;
             row <- 0 until upperBound;
                t <- 0 until upperBound) yield {
          zst.toIndex(SpaceTimeKey(row,col,y2k.plusYears(t)))
        }

      keys.distinct.size should be (upperBound * upperBound * upperBound)
      keys.min should be (zst.toIndex(SpaceTimeKey(0,0,y2k)))
      keys.max should be (zst.toIndex(SpaceTimeKey(upperBound-1, upperBound-1, y2k.plusYears(upperBound-1))))
    }

    it("generates indexes you can check by hand 2x2x2"){
     val zst = ZSpaceTimeKeyIndex({dt => dt.getMillis.toInt})
     val idx = List[SpaceTimeKey](
                                  SpaceTimeKey(0,0, y2k),
                                  SpaceTimeKey(1,0, y2k),
                                  SpaceTimeKey(0,1, y2k),
                                  SpaceTimeKey(1,1, y2k),

                                  SpaceTimeKey(0,0, y2k.plusMillis(1)),
                                  SpaceTimeKey(1,0, y2k.plusMillis(1)),
                                  SpaceTimeKey(0,1, y2k.plusMillis(1)),
                                  SpaceTimeKey(1,1, y2k.plusMillis(1))
                                 )
     for(i <- 0 to 6){
	zst.toIndex(idx(i)) should be (zst.toIndex(idx(i+1)) - 1)
     }
     zst.toIndex(idx(6)) should be (zst.toIndex(idx(7)) - 1)
    }

    it("generates a Seq[(Long,Long)] from a keyRange: (SpaceTimeKey, SpaceTimeKey)"){
      val zst = ZSpaceTimeKeyIndex({dt => dt.getMillis.toInt})

      //all sub cubes in a 2x2x2
      var idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (7) 
    
      //sub cubes along x
      idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,0,y2k.plusMillis(1))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1) 
      (idx(1)._2 - idx(1)._1) should be (1) 
    
      //next sub cubes along x
      idx = zst.indexRanges((SpaceTimeKey(0,1,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1) 
      (idx(1)._2 - idx(1)._1) should be (1) 
    
      //sub cubes along y
      idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,1,y2k.plusMillis(1))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0) 
      (idx(1)._2 - idx(1)._1) should be (0) 
      (idx(2)._2 - idx(2)._1) should be (0) 
      (idx(3)._2 - idx(3)._1) should be (0) 
    
      //next sub cubes along y
      idx = zst.indexRanges((SpaceTimeKey(1,0,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0) 
      (idx(1)._2 - idx(1)._1) should be (0) 
      (idx(2)._2 - idx(2)._1) should be (0) 
      (idx(3)._2 - idx(3)._1) should be (0) 
    
      //sub cubes along z
      idx = zst.indexRanges( (SpaceTimeKey(0,0,y2k.plusMillis(1)), SpaceTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (3) 
    
      //sub cubes along z
      idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k)))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (3) 
    }

    it("generates indexes by string pattern") {
      val zst = ZSpaceTimeKeyIndex.byPattern(ymPattern)

      val keys =
        for(col <- 0 until upperBound;
            row <- 0 until upperBound;
            t <- 0 until upperBound) yield {
          zst.toIndex(SpaceTimeKey(row,col,y2k.plusMonths(t)))
        }

        keys.distinct.size should be (upperBound * upperBound * upperBound)
        keys.min should be (zst.toIndex(SpaceTimeKey(0,0,y2k)))
        keys.max should be (zst.toIndex(SpaceTimeKey(upperBound-1, upperBound-1, y2k.plusMonths(upperBound-1))))
    }
  }
}
