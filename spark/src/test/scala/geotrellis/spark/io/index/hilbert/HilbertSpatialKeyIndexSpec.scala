package geotrellis.spark.io.index.hilbert

import org.scalatest._
import scala.collection.immutable.TreeSet
import geotrellis.spark.SpatialKey

class HilbertSpatialKeyIndexSpec extends FunSpec with Matchers{

  val UpperBound: Int = 64

  describe("HilbertSpatialKeyIndex tests"){

    it("Generates a Long index given a SpatialKey"){
     val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(UpperBound,UpperBound), 6) //what are the SpatialKeys used for?

       var i = 0
       var j = 0
       var ts: TreeSet[Long] = TreeSet()

       while (i < UpperBound) {
        j=0
          while(j < UpperBound){

            var idx = hilbert.toIndex(SpatialKey(j,i))
            var x: Option[Long] = ts.find(y => y == idx)

            x.isEmpty should be (true) //add element exactly once
            ts = ts + idx

            j+=1
        }
        i+=1
       }

       //check size
       ts.size should be (UpperBound * UpperBound)

       //check for consecutivity
       val itr: Iterator[Long] = ts.iterator
       var s = itr.next
       while(itr.hasNext){
        var t = itr.next
        t should be (s+1)
        s = t
       }

     
    }

    it("generates hand indexes you can hand check 2x2"){
     val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(UpperBound,UpperBound), 1) 
     //right oriented
     hilbert.toIndex(SpatialKey(0,0)) should be (0) // (0,0)
     hilbert.toIndex(SpatialKey(1,0)) should be (1) // (0,1)
     hilbert.toIndex(SpatialKey(0,1)) should be (3) // (1,0)
     hilbert.toIndex(SpatialKey(1,1)) should be (2) // (1,1)
    }

    it("generates hand indexes you can hand check 4x4"){
     val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(UpperBound,UpperBound), 2) //what are the SpatialKeys used for?
     val grid = List[SpatialKey]( SpatialKey(0,0), SpatialKey(1,0), SpatialKey(1,1), SpatialKey(0,1), 
                                  SpatialKey(0,2), SpatialKey(0,3), SpatialKey(1,3), SpatialKey(1,2), 
                                  SpatialKey(2,2), SpatialKey(2,3), SpatialKey(3,3), SpatialKey(3,2), 
                                  SpatialKey(3,1), SpatialKey(2,1), SpatialKey(2,0), SpatialKey(3,0)
                                )
     for{i<- 0 to 15}
	hilbert.toIndex(grid(i)) should be (i)
    }

    it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){

     //hand checked examples
     val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(UpperBound,UpperBound), 2) 
  
     //square grids
     var idx = hilbert.indexRanges( (SpatialKey(0,0), SpatialKey(2,2)) ) 
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (4)

     idx = hilbert.indexRanges( (SpatialKey(0,0), SpatialKey(4,4)) ) 
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (16)
     
     //check some subgrids
     idx = hilbert.indexRanges( (SpatialKey(2,1), SpatialKey(4,3)) ) 
     idx.length should be (2)
     idx(0)._1 should be (8)
     idx(0)._2 should be (9)
     idx(1)._1 should be (11)
     idx(1)._2 should be (14)

     idx = hilbert.indexRanges( (SpatialKey(1,1), SpatialKey(3,3)) ) 
     idx.length should be (3)
     idx(0)._1 should be (2)
     idx(0)._2 should be (3)
     idx(1)._1 should be (7)
     idx(1)._2 should be (9)
     idx(2)._1 should be (13)
     idx(2)._2 should be (14)

     //check some rows
     idx = hilbert.indexRanges( (SpatialKey(0,0), SpatialKey(4,1)) ) 
     idx.length should be (2)
     idx(0)._1 should be (0)
     idx(0)._2 should be (2)
     idx(1)._1 should be (14)
     idx(1)._2 should be (16)

     idx = hilbert.indexRanges( (SpatialKey(0,2), SpatialKey(4,3)) ) 
     idx.length should be (3)
     idx(0)._1 should be (4)
     idx(0)._2 should be (5)
     idx(1)._1 should be (7)
     idx(1)._2 should be (9)
     idx(2)._1 should be (11)
     idx(2)._2 should be (12)

     //check some cols
     idx = hilbert.indexRanges( (SpatialKey(1,0), SpatialKey(2,4)) ) 
     idx.length should be (2)
     idx(0)._1 should be (1)
     idx(0)._2 should be (3)
     idx(1)._1 should be (6)
     idx(1)._2 should be (8)

     idx = hilbert.indexRanges( (SpatialKey(3,0), SpatialKey(4,4)) ) 
     idx.length should be (2)
     idx(0)._1 should be (10)
     idx(0)._2 should be (13)
     idx(1)._1 should be (15)
     idx(1)._2 should be (16)

    }


  }
}
