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
     var idx = zsk.indexRanges(SpatialKey(0,0), SpatialKey(10,10))

    }
  }
}

