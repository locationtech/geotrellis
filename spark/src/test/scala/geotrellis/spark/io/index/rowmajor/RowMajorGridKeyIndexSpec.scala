package geotrellis.spark.io.index.rowmajor

import scala.collection.immutable.TreeSet
import org.scalatest._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.KeyBounds
import geotrellis.spark.GridKey

class RowMajorGridKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 64

  describe("RowMajorGridKeyIndex tests") {

    it("Generates a Long index given a GridKey"){

      val rmajor = new RowMajorGridKeyIndex(KeyBounds[GridKey](GridKey(0,0), GridKey(upperBound-1, upperBound-1)))
   
      val keys = 
        for(col <- 0 until upperBound;
             row <- 0 until upperBound) yield {
          rmajor.toIndex(GridKey(col, row))
        }
   
      keys.distinct.size should be (upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound - 1)
    }
      
      
    it("generates indexes you can check by hand 2x2"){
      val rmajor = new RowMajorGridKeyIndex(KeyBounds[GridKey](GridKey(0,0), GridKey(1,1)))
      val grid = List[GridKey](GridKey(0,0), GridKey(1,0), GridKey(0,1), GridKey(1,1))
       rmajor.toIndex(grid(0)) should be(0)
       rmajor.toIndex(grid(1)) should be(1)
       rmajor.toIndex(grid(2)) should be(2)
       rmajor.toIndex(grid(3)) should be(3)
    }

    it("generates indexes you can check by hand 4x4"){
      val rmajor = new RowMajorGridKeyIndex(KeyBounds[GridKey](GridKey(0,0), GridKey(3,3)))
      var grid = List[GridKey]()
      for{ i <- 0 to 3; j <- 0 to 3} 
        rmajor.toIndex(GridKey(j,i)) should be (4*i+j)
    }

    it("Generates a Seq[(Long,Long)] given a key range (GridKey,GridKey)"){
      val rmajor = new RowMajorGridKeyIndex(KeyBounds[GridKey](GridKey(0,0), GridKey(3,3)))

      //checked by hand 4x4

      //cols
      var idx = rmajor.indexRanges((GridKey(1,0), GridKey(1,3)))
      idx.length should be(4)
      idx(0)._1 should be (1)
      idx(0)._2 should be (1)
      idx(1)._1 should be (5)
      idx(1)._2 should be (5)
      idx(2)._1 should be (9)
      idx(2)._2 should be (9)
      idx(3)._1 should be (13)
      idx(3)._2 should be (13)

      idx = rmajor.indexRanges((GridKey(0,0), GridKey(0,3)))
      idx.length should be(4)
      idx(0)._1 should be (0)
      idx(0)._2 should be (0)
      idx(1)._1 should be (4)
      idx(1)._2 should be (4)
      idx(2)._1 should be (8)
      idx(2)._2 should be (8)
      idx(3)._1 should be (12)
      idx(3)._2 should be (12)

      idx = rmajor.indexRanges((GridKey(2,0), GridKey(2,3)))
      idx.length should be(4)
      idx(0)._1 should be (2)
      idx(0)._2 should be (2)
      idx(1)._1 should be (6)
      idx(1)._2 should be (6)
      idx(2)._1 should be (10)
      idx(2)._2 should be (10)
      idx(3)._1 should be (14)
      idx(3)._2 should be (14)

      idx = rmajor.indexRanges((GridKey(3,0), GridKey(3,3)))
      idx.length should be(4)
      idx(0)._1 should be (3)
      idx(0)._2 should be (3)
      idx(1)._1 should be (7)
      idx(1)._2 should be (7)
      idx(2)._1 should be (11)
      idx(2)._2 should be (11)
      idx(3)._1 should be (15)
      idx(3)._2 should be (15)

      //rows
      idx = rmajor.indexRanges((GridKey(0,0), GridKey(3,0)))
      idx.length should be(1)
      idx(0)._1 should be (0)
      idx(0)._2 should be (3)

      idx = rmajor.indexRanges((GridKey(0,1), GridKey(3,1)))
      idx.length should be(1)
      idx(0)._1 should be (4)
      idx(0)._2 should be (7)

      idx = rmajor.indexRanges((GridKey(0,2), GridKey(3,2)))
      idx.length should be(1)
      idx(0)._1 should be (8)
      idx(0)._2 should be (11)

      idx = rmajor.indexRanges((GridKey(0,3), GridKey(3,3)))
      idx.length should be(1)
      idx(0)._1 should be (12)
      idx(0)._2 should be (15)

      //subgrids
      idx = rmajor.indexRanges((GridKey(0,0), GridKey(2,1)))
      idx.length should be(2)
      idx(0)._1 should be (0)
      idx(0)._2 should be (2)
      idx(1)._1 should be (4)
      idx(1)._2 should be (6)

      idx = rmajor.indexRanges((GridKey(0,0), GridKey(1,2)))
      idx.length should be(3)
      idx(0)._1 should be (0)
      idx(0)._2 should be (1)
      idx(1)._1 should be (4)
      idx(1)._2 should be (5)
      idx(2)._1 should be (8)
      idx(2)._2 should be (9)

      idx = rmajor.indexRanges((GridKey(1,0), GridKey(2,1)))
      idx.length should be(2)
      idx(0)._1 should be (1)
      idx(0)._2 should be (2)
      idx(1)._1 should be (5)
      idx(1)._2 should be (6)
    }
  }
}
