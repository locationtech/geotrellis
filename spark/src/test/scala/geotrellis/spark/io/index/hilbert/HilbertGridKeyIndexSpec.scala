package geotrellis.spark.io.index.hilbert

import org.scalatest._
import scala.collection.immutable.TreeSet
import geotrellis.spark.GridKey

class HilbertGridKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 64

  describe("HilbertGridKeyIndex tests"){

    it("Generates a Long index given a GridKey"){
      val hilbert = HilbertGridKeyIndex(GridKey(0,0), GridKey(upperBound,upperBound), 6) //what are the GridKeys used for?

      val keys = 
        for(col <- 0 until upperBound;
           row <- 0 until upperBound) yield {
          hilbert.toIndex(GridKey(col,row))
        }
    
      keys.distinct.size should be (upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound - 1)
    }

    it("generates hand indexes you can hand check 2x2"){
     val hilbert = HilbertGridKeyIndex(GridKey(0,0), GridKey(upperBound,upperBound), 1)
     //right oriented
     hilbert.toIndex(GridKey(0,0)) should be (0)
     hilbert.toIndex(GridKey(0,1)) should be (1)
     hilbert.toIndex(GridKey(1,1)) should be (2)
     hilbert.toIndex(GridKey(1,0)) should be (3)
    }

    it("generates hand indexes you can hand check 4x4"){
     val hilbert = HilbertGridKeyIndex(GridKey(0,0), GridKey(upperBound,upperBound), 2) //what are the GridKeys used for?
     val grid = List[GridKey]( GridKey(0,0), GridKey(1,0), GridKey(1,1), GridKey(0,1),
                                  GridKey(0,2), GridKey(0,3), GridKey(1,3), GridKey(1,2),
                                  GridKey(2,2), GridKey(2,3), GridKey(3,3), GridKey(3,2),
                                  GridKey(3,1), GridKey(2,1), GridKey(2,0), GridKey(3,0))
     for(i <- 0 to 15){
       hilbert.toIndex(grid(i)) should be (i)
     }
    }

    it("Generates a Seq[(Long,Long)] given a key range (GridKey,GridKey)"){
        //hand re-checked examples
        val hilbert = HilbertGridKeyIndex(GridKey(0,0), GridKey(upperBound,upperBound), 2)

        // single point, min corner
        var idx = hilbert.indexRanges((GridKey(0,0), GridKey(0,0)))
        idx.length should be (1)
        idx.toSet should be (Set(0->0))

        // single point, max corner
        idx = hilbert.indexRanges((GridKey(3,3), GridKey(3,3)))
        idx.length should be (1)
        idx.toSet should be (Set(10->10)) // aha, not 15 as you might think!

        //square grids
        idx = hilbert.indexRanges((GridKey(0,0), GridKey(1,1)))
        idx.length should be (1)
        idx.toSet should be (Set(0->3))

        idx = hilbert.indexRanges((GridKey(0,0), GridKey(3,3)))
        idx.length should be (1)
        idx.toSet should be (Set(0->15))

        //check some subgrid
        idx = hilbert.indexRanges((GridKey(1,0), GridKey(3,2)))
        idx.length should be (3)
        idx.toSet should be (Set(1->2, 7->8, 11->15))
     }
  }
}
