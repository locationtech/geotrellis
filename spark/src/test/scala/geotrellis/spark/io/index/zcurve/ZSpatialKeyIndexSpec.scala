package geotrellis.spark.io.index.zcurve

import scala.collection.immutable.TreeSet
import org.scalatest._
import geotrellis.spark.SpatialKey

class ZSpatialKeyIndexSpec extends FunSpec with Matchers {

  val UpperBound = 64

  describe("ZSpatialKeyIndex test") {
    it("generates an index from a SpatialKey"){
    
     val zsk = new ZSpatialKeyIndex()
     var i = 0
     var j = 0
     var ts: TreeSet[Long] = TreeSet()

     while (i < UpperBound) {
      j=0
        while(j < UpperBound){

          var idx = zsk.toIndex(SpatialKey(i,j))
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

    it("generates indexes you can hand check 2x2"){
     val zsk = new ZSpatialKeyIndex()
     val keys= List[SpatialKey](SpatialKey(0,0),SpatialKey(1,0),
                                SpatialKey(0,1),SpatialKey(1,1)
                               ) 

     for{i <- 0 to 3}  
       zsk.toIndex(keys(i)) should be( i )
    }


    it("generates indexes you can hand check 4x4"){
     val zsk = new ZSpatialKeyIndex()
     val keys= List[SpatialKey](SpatialKey(0,0),SpatialKey(1,0),
                                SpatialKey(0,1),SpatialKey(1,1),
                                SpatialKey(2,0),SpatialKey(3,0),
                                SpatialKey(2,1),SpatialKey(3,1),
                                SpatialKey(0,2),SpatialKey(1,2),
                                SpatialKey(0,3),SpatialKey(1,3),
                                SpatialKey(2,2),SpatialKey(3,2),
                                SpatialKey(2,3),SpatialKey(3,3)
                               )
     
     for{i <- 0 to 15}  
       zsk.toIndex(keys(i)) should be( i )

    }

    it("generates a Seq[(Long, Long)] from a keyRange (SpatialKey,SpatialKey)"){
     val zsk = new ZSpatialKeyIndex()

     //checked by hand 4x4
     var idx: Seq[(Long,Long)] = zsk.indexRanges((SpatialKey(0,0), SpatialKey(1,1)))
     idx.length should be( 1 )
     idx(0)._1 should be( 0 )
     idx(0)._2 should be( 3 )

     idx = zsk.indexRanges( (SpatialKey(0,0), SpatialKey(0,0) ) )
     idx.length should be( 1 )
     idx(0)._1 should be( idx(0)._2)
     idx(0)._1 should be( 0 )

     idx = zsk.indexRanges( (SpatialKey(2,0), SpatialKey(3,1) ) )
     idx.length should be( 1 )
     println(idx)
     idx(0)._1 should be( 4 )
     idx(0)._2 should be( 7 )

     idx = zsk.indexRanges( (SpatialKey(0,2), SpatialKey(1,3) ) )
     idx.length should be( 1 )
     idx(0)._1 should be( 8 )
     idx(0)._2 should be( 11 )

     idx = zsk.indexRanges( (SpatialKey(2,2), SpatialKey(3,3) ) )
     idx.length should be( 1 )
     idx(0)._1 should be( 12 )
     idx(0)._2 should be( 15 )

     idx = zsk.indexRanges( (SpatialKey(0,0), SpatialKey(3,3) ) )
     idx.length should be( 1 )
     idx(0)._1 should be( 0 )
     idx(0)._2 should be( 15 )

     //check non-consecutive cases
     idx = zsk.indexRanges( (SpatialKey(0,0), SpatialKey(2,1) ) )
     idx.length should be( 2 )
     idx(0)._1 should be( 0 )
     idx(0)._2 should be( 4 )
     idx(1)._1 should be (idx(1)._2)
     idx(1)._1 should be( 6 )


     idx = zsk.indexRanges( (SpatialKey(0,0), SpatialKey(1,2) ) )
     idx.length should be( 2 )
     idx(0)._1 should be( 0 )
     idx(0)._2 should be( 3 )
     idx(1)._1 should be( 8 )
     idx(1)._2 should be( 9 )

     idx = zsk.indexRanges( (SpatialKey(0,0), SpatialKey(1,2) ) )
     idx.length should be( 2 )
     idx(0)._1 should be( 0 )
     idx(0)._2 should be( 3 )
     idx(1)._1 should be( 8 )
     idx(1)._2 should be( 9 )

     idx = zsk.indexRanges( (SpatialKey(0,0), SpatialKey(1,2) ) )
     idx.length should be( 2 )
     idx(0)._1 should be( 0 )
     idx(0)._2 should be( 3 )
     idx(1)._1 should be( 8 )
     idx(1)._2 should be( 9 )

     idx = zsk.indexRanges( (SpatialKey(1,1), SpatialKey(3,2) ) )
     idx.length should be( 4 )
     idx(0)._1 should be( 3 )
     idx(0)._2 should be( 3 )
     idx(1)._1 should be( 6 )
     idx(1)._2 should be( 7 )
     idx(2)._1 should be( 9 )
     idx(2)._2 should be( 9 )
     idx(3)._1 should be( 12 )
     idx(3)._2 should be( 13 )

     idx = zsk.indexRanges( (SpatialKey(2,0), SpatialKey(2,3) ) )
     idx.length should be( 4 )
     idx(0)._1 should be( 4 )
     idx(0)._2 should be( 4 )
     idx(1)._1 should be( 6 )
     idx(1)._2 should be( 6 )
     idx(2)._1 should be( 12 )
     idx(2)._2 should be( 12 )
     idx(3)._1 should be( 14 )
     idx(3)._2 should be( 14 )

    }
  }
}

