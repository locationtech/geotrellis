package geotrellis.spark.io.index.rowmajor

import scala.collection.immutable.TreeSet
import org.scalatest._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.KeyBounds
import geotrellis.spark.SpatialKey

class RowMajorSpatialKeyIndexSpec extends FunSpec with Matchers{

  val UpperBound: Int = 128

  describe("RowMajorSpatialKeyIndex tests") {

    it("Generates a Long index given a SpatialKey"){
      //rowmajor for a 128x128 grid
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(UpperBound-1, UpperBound-1))) 

       var i = 0
       var j = 0
       var ts: TreeSet[Long] = TreeSet()
      
       while (i < UpperBound) {
        j=0
          while(j < UpperBound){
      
            var idx = rmajor.toIndex(SpatialKey(j,i))
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
      
      
    it("generates indexes you can check by hand 2x2"){
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(1, 1))) 
      val grid = List[SpatialKey](SpatialKey(0,0), SpatialKey(1,0), SpatialKey(0,1), SpatialKey(1,1)) //see SpatialKey
       rmajor.toIndex(grid(0)) should be( 0 )
       rmajor.toIndex(grid(1)) should be( 1 )
       rmajor.toIndex(grid(2)) should be( 2 )
       rmajor.toIndex(grid(3)) should be( 3 )
    }

    it("generates indexes you can check by hand 4x4"){
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(3, 3))) //see SpatialKey
      var grid = List[SpatialKey]()
      for{ i <- 0 to 3; j <- 0 to 3} 
        rmajor.toIndex(SpatialKey(j,i)) should be (4*i+j)
    }

    it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(3, 3))) //see SpatialKey

      //checked by hand 4x4

      //cols
      var idx = rmajor.indexRanges( ( SpatialKey(1,0), SpatialKey(1,3) ) )
      idx.length should be( 4 )
      idx(0)._1 should be (1 )
      idx(0)._2 should be (2 )
      idx(1)._1 should be (5 )
      idx(1)._2 should be (6 )
      idx(2)._1 should be (9 )
      idx(2)._2 should be (10 )
      idx(3)._1 should be (13 )
      idx(3)._2 should be (14 )

      idx = rmajor.indexRanges( ( SpatialKey(0,0), SpatialKey(0,3) ) )
      idx.length should be( 4 )
      idx(0)._1 should be (0 )
      idx(0)._2 should be (1 )
      idx(1)._1 should be (4 )
      idx(1)._2 should be (5 )
      idx(2)._1 should be (8 )
      idx(2)._2 should be (9 )
      idx(3)._1 should be (12 )
      idx(3)._2 should be (13 )

      idx = rmajor.indexRanges( ( SpatialKey(2,0), SpatialKey(2,3) ) )
      idx.length should be( 4 )
      idx(0)._1 should be (2 )
      idx(0)._2 should be (3 )
      idx(1)._1 should be (6 )
      idx(1)._2 should be (7 )
      idx(2)._1 should be (10 )
      idx(2)._2 should be (11 )
      idx(3)._1 should be (14 )
      idx(3)._2 should be (15 )

      idx = rmajor.indexRanges( ( SpatialKey(3,0), SpatialKey(3,3) ) )
      idx.length should be( 4 )
      idx(0)._1 should be (3)
      idx(0)._2 should be (4 )
      idx(1)._1 should be (7 )
      idx(1)._2 should be (8 )
      idx(2)._1 should be (11 )
      idx(2)._2 should be (12 )
      idx(3)._1 should be (15 )
      idx(3)._2 should be (16 )

      //rows
      idx = rmajor.indexRanges( ( SpatialKey(0,0), SpatialKey(3,0) ) )
      idx.length should be( 1 )
      idx(0)._1 should be (0)
      idx(0)._2 should be (4)

      idx = rmajor.indexRanges( ( SpatialKey(0,1), SpatialKey(3,1) ) )
      idx.length should be( 1 )
      idx(0)._1 should be (4)
      idx(0)._2 should be (8)

      idx = rmajor.indexRanges( ( SpatialKey(0,2), SpatialKey(3,2) ) )
      idx.length should be( 1 )
      idx(0)._1 should be (8)
      idx(0)._2 should be (12)

      idx = rmajor.indexRanges( ( SpatialKey(0,3), SpatialKey(3,3) ) )
      idx.length should be( 1 )
      idx(0)._1 should be (12)
      idx(0)._2 should be (16)

      //subgrids
      idx = rmajor.indexRanges( ( SpatialKey(0,0), SpatialKey(2,1) ) )
      idx.length should be( 2 )
      idx(0)._1 should be (0)
      idx(0)._2 should be (3)
      idx(1)._1 should be (4)
      idx(1)._2 should be (7)

      idx = rmajor.indexRanges( ( SpatialKey(0,0), SpatialKey(1,2) ) )
      idx.length should be( 3 )
      idx(0)._1 should be (0)
      idx(0)._2 should be (2)
      idx(1)._1 should be (4)
      idx(1)._2 should be (6)
      idx(2)._1 should be (8)
      idx(2)._2 should be (10)

      idx = rmajor.indexRanges( ( SpatialKey(1,0), SpatialKey(2,1) ) )
      idx.length should be( 2 )
      idx(0)._1 should be (1)
      idx(0)._2 should be (3)
      idx(1)._1 should be (5)
      idx(1)._2 should be (7)


    }
  }
}
