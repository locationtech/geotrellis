package geotrellis.spark.io.index.zcurve

import geotrellis.spark._

import org.scalatest._

import scala.collection.immutable.TreeSet

class ZGridKeyIndexSpec extends FunSpec with Matchers {

  val upperBound = 64
  val keyBounds = KeyBounds(GridKey(0, 0), GridKey(100, 100))

  describe("ZGridKeyIndex test") {
    it("generates an index from a GridKey"){

      val zsk = new ZGridKeyIndex(keyBounds)

      val keys =
        for(col <- 0 until upperBound;
             row <- 0 until upperBound) yield {
          zsk.toIndex(GridKey(col, row))
        }

      keys.distinct.size should be (upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound - 1)
    }

    it("generates indexes you can hand check 2x2"){
     val zsk = new ZGridKeyIndex(keyBounds)
     val keys= List[GridKey](GridKey(0,0),GridKey(1,0),
                                GridKey(0,1),GridKey(1,1))

     for(i <- 0 to 3){
       zsk.toIndex(keys(i)) should be(i)
     }
    }


    it("generates indexes you can hand check 4x4"){
     val zsk = new ZGridKeyIndex(keyBounds)
     val keys= List[GridKey](GridKey(0,0),GridKey(1,0),
                                GridKey(0,1),GridKey(1,1),
                                GridKey(2,0),GridKey(3,0),
                                GridKey(2,1),GridKey(3,1),
                                GridKey(0,2),GridKey(1,2),
                                GridKey(0,3),GridKey(1,3),
                                GridKey(2,2),GridKey(3,2),
                                GridKey(2,3),GridKey(3,3))

     for(i <- 0 to 15){
       zsk.toIndex(keys(i)) should be(i)
     }
    }

    it("generates a Seq[(Long, Long)] from a keyRange (GridKey,GridKey)"){
     val zsk = new ZGridKeyIndex(keyBounds)

     //checked by hand 4x4
     var idx: Seq[(Long,Long)] = zsk.indexRanges((GridKey(0,0), GridKey(1,1)))
     idx.length should be(1)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)

     idx = zsk.indexRanges((GridKey(0,0), GridKey(0,0)))
     idx.length should be(1)
     idx(0)._1 should be(idx(0)._2)
     idx(0)._1 should be(0)

     idx = zsk.indexRanges((GridKey(2,0), GridKey(3,1)))
     idx.length should be(1)
     idx(0)._1 should be(4)
     idx(0)._2 should be(7)

     idx = zsk.indexRanges((GridKey(0,2), GridKey(1,3)))
     idx.length should be(1)
     idx(0)._1 should be(8)
     idx(0)._2 should be(11)

     idx = zsk.indexRanges((GridKey(2,2), GridKey(3,3)))
     idx.length should be(1)
     idx(0)._1 should be(12)
     idx(0)._2 should be(15)

     idx = zsk.indexRanges((GridKey(0,0), GridKey(3,3)))
     idx.length should be(1)
     idx(0)._1 should be(0)
     idx(0)._2 should be(15)

     //check non-consecutive cases
     idx = zsk.indexRanges((GridKey(0,0), GridKey(2,1)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(4)
     idx(1)._1 should be (idx(1)._2)
     idx(1)._1 should be(6)


     idx = zsk.indexRanges((GridKey(0,0), GridKey(1,2)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)
     idx(1)._1 should be(8)
     idx(1)._2 should be(9)

     idx = zsk.indexRanges((GridKey(0,0), GridKey(1,2)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)
     idx(1)._1 should be(8)
     idx(1)._2 should be(9)

     idx = zsk.indexRanges((GridKey(0,0), GridKey(1,2)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)
     idx(1)._1 should be(8)
     idx(1)._2 should be(9)

     idx = zsk.indexRanges((GridKey(1,1), GridKey(3,2)))
     idx.length should be(4)
     idx(0)._1 should be(3)
     idx(0)._2 should be(3)
     idx(1)._1 should be(6)
     idx(1)._2 should be(7)
     idx(2)._1 should be(9)
     idx(2)._2 should be(9)
     idx(3)._1 should be(12)
     idx(3)._2 should be(13)

     idx = zsk.indexRanges((GridKey(2,0), GridKey(2,3)))
     idx.length should be(4)
     idx(0)._1 should be(4)
     idx(0)._2 should be(4)
     idx(1)._1 should be(6)
     idx(1)._2 should be(6)
     idx(2)._1 should be(12)
     idx(2)._2 should be(12)
     idx(3)._1 should be(14)
     idx(3)._2 should be(14)
    }
  }
}
