package geotrellis.spark.io.index.zcurve

import geotrellis.spark._

import org.joda.time.DateTime
import org.scalatest._
import scala.collection.immutable.TreeSet

class ZGridTimeKeySpec extends FunSpec with Matchers{
  val y2k = new DateTime(2000, 1, 1, 0, 0)
  val upperBound = 8
  val keyBounds = KeyBounds(GridTimeKey(0, 0, 0L), GridTimeKey(100, 100, 100L))

  describe("ZGridTimeKey test"){

    it("indexes time"){
      val zst = ZGridTimeKeyIndex.byYear(keyBounds)

      val keys =
        for(col <- 0 until upperBound;
             row <- 0 until upperBound;
                t <- 0 until upperBound) yield {
          zst.toIndex(GridTimeKey(row,col,y2k.plusYears(t)))
        }

      keys.distinct.size should be (upperBound * upperBound * upperBound)
      keys.min should be (zst.toIndex(GridTimeKey(0,0,y2k)))
      keys.max should be (zst.toIndex(GridTimeKey(upperBound-1, upperBound-1, y2k.plusYears(upperBound-1))))
    }

    it("generates indexes you can check by hand 2x2x2"){
     val zst = ZGridTimeKeyIndex.byMilliseconds(keyBounds, 1)
     val idx = List[GridTimeKey](
                                  GridTimeKey(0,0, y2k),
                                  GridTimeKey(1,0, y2k),
                                  GridTimeKey(0,1, y2k),
                                  GridTimeKey(1,1, y2k),

                                  GridTimeKey(0,0, y2k.plusMillis(1)),
                                  GridTimeKey(1,0, y2k.plusMillis(1)),
                                  GridTimeKey(0,1, y2k.plusMillis(1)),
                                  GridTimeKey(1,1, y2k.plusMillis(1))
                                 )
     for(i <- 0 to 6){
	zst.toIndex(idx(i)) should be (zst.toIndex(idx(i+1)) - 1)
     }
     zst.toIndex(idx(6)) should be (zst.toIndex(idx(7)) - 1)
    }

    it("generates a Seq[(Long,Long)] from a keyRange: (GridTimeKey, GridTimeKey)"){
     val zst = ZGridTimeKeyIndex.byMilliseconds(keyBounds, 1)

      //all sub cubes in a 2x2x2
      var idx = zst.indexRanges((GridTimeKey(0,0,y2k), GridTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (7)

      //sub cubes along x
      idx = zst.indexRanges((GridTimeKey(0,0,y2k), GridTimeKey(1,0,y2k.plusMillis(1))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1)
      (idx(1)._2 - idx(1)._1) should be (1)

      //next sub cubes along x
      idx = zst.indexRanges((GridTimeKey(0,1,y2k), GridTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1)
      (idx(1)._2 - idx(1)._1) should be (1)

      //sub cubes along y
      idx = zst.indexRanges((GridTimeKey(0,0,y2k), GridTimeKey(0,1,y2k.plusMillis(1))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0)
      (idx(1)._2 - idx(1)._1) should be (0)
      (idx(2)._2 - idx(2)._1) should be (0)
      (idx(3)._2 - idx(3)._1) should be (0)

      //next sub cubes along y
      idx = zst.indexRanges((GridTimeKey(1,0,y2k), GridTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0)
      (idx(1)._2 - idx(1)._1) should be (0)
      (idx(2)._2 - idx(2)._1) should be (0)
      (idx(3)._2 - idx(3)._1) should be (0)

      //sub cubes along z
      idx = zst.indexRanges( (GridTimeKey(0,0,y2k.plusMillis(1)), GridTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (3)

      //sub cubes along z
      idx = zst.indexRanges((GridTimeKey(0,0,y2k), GridTimeKey(1,1,y2k)))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (3)
    }

    it("generates indexes by month") {
      val zst = ZGridTimeKeyIndex.byMonth(keyBounds)

      val keys =
        for(col <- 0 until upperBound;
            row <- 0 until upperBound;
            t <- 0 until upperBound) yield {
          zst.toIndex(GridTimeKey(row,col,y2k.plusMonths(t)))
        }

        keys.distinct.size should be (upperBound * upperBound * upperBound)
        keys.min should be (zst.toIndex(GridTimeKey(0,0,y2k)))
        keys.max should be (zst.toIndex(GridTimeKey(upperBound-1, upperBound-1, y2k.plusMonths(upperBound-1))))
    }
  }
}
