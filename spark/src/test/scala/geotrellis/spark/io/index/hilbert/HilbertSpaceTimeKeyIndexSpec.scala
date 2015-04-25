package geotrellis.spark.io.index.hilbert

import org.scalatest._
import org.joda.time.DateTime
import scala.collection.immutable.TreeSet
import geotrellis.spark.SpaceTimeKey
import geotrellis.spark.KeyBounds

class HilbertSpaceTimeKeyIndexSpec extends FunSpec with Matchers{

  val UpperBound: Int = 16
  val y2k = new DateTime(2000,1,1,0,0,0,0)
  describe("HilbertSpaceTimeKeyIndex tests"){

    it("indexes col, row, and time"){
     val hst = HilbertSpaceTimeKeyIndex(
                        SpaceTimeKey(0,0,y2k),  
                        SpaceTimeKey(0,0,y2k.plusMillis(5)),5 , 5)
     var i = 0
     var j = 0
     var k = 0
     var ts: TreeSet[Long] = TreeSet()
     
     while (i < UpperBound) {
      j=0
        while(j < UpperBound){
         k=0
           while(k < UpperBound){
              var idx = hst.toIndex(SpaceTimeKey(j,k,y2k.plusMillis(i)))
              var x: Option[Long] = ts.find(y => y == idx)
              x.isEmpty should be (true) //add element exactly once
              ts = ts + idx 
           
              k+=1
           }
          j+=1 
      }
      i+=1
     }
     
     //check size
     ts.size should be (UpperBound * UpperBound * UpperBound)
     
     //check for consecutivity
     val itr: Iterator[Long] = ts.iterator
     var s = itr.next
     while(itr.hasNext){
      var t = itr.next 
      t should be (s+1)
      s = t
     }
    }

   it("generates hand indexes you can hand check 2x2x2"){
     val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(1)),1,1) 
     val idx = List[SpaceTimeKey]( SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,1,y2k),
                                   SpaceTimeKey(1,1,y2k), SpaceTimeKey(1,0,y2k),
                                   SpaceTimeKey(1,0,y2k.plusMillis(1)), SpaceTimeKey(1,1,y2k.plusMillis(1)),
                                   SpaceTimeKey(0,1,y2k.plusMillis(1)), SpaceTimeKey(0,0,y2k.plusMillis(1)))
     
     for{ i<-0 to 7 }
       hilbert.toIndex(idx(i)) should be (i)

    }

   it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){

     //hand checked examples for a 2x2x2
     val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), 
                                          SpaceTimeKey(0,0,y2k.plusMillis(2)),
	                                  2, 2)
     //2x2x2 cube
     var idx = hilbert.indexRanges( ( SpaceTimeKey(0,0,y2k),
                                      SpaceTimeKey(2,2,y2k.plusMillis(2)) )
                                  )
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (8)

     //first 4 sub cubes (along y)
     idx = hilbert.indexRanges( ( SpaceTimeKey(0,0,y2k),
                                  SpaceTimeKey(1,2,y2k.plusMillis(2)) )
                                )
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (4)

     //second 4 sub cubes (along y)
     idx = hilbert.indexRanges( ( SpaceTimeKey(1,0,y2k),
                                  SpaceTimeKey(2,2,y2k.plusMillis(2)) )
                              )
     idx.length should be (1)
     idx(0)._1 should be (4)
     idx(0)._2 should be (8)

     //4 sub cubes (along x)
     idx = hilbert.indexRanges( ( SpaceTimeKey(0,1,y2k),
                                  SpaceTimeKey(2,2,y2k.plusMillis(2)) )
                              )
     idx.length should be (1)
     idx(0)._1 should be (2)
     idx(0)._2 should be (6)

     //next 4 sub cubes (along x)
     idx = hilbert.indexRanges( ( SpaceTimeKey(0,0,y2k),
                                  SpaceTimeKey(2,1,y2k.plusMillis(2)) )
                              )
     idx.length should be (2)
     idx(0)._1 should be (0)
     idx(0)._2 should be (2)
     idx(1)._1 should be (6)
     idx(1)._2 should be (8)

     //single point
     idx = hilbert.indexRanges( ( SpaceTimeKey(0,0,y2k),
                                      SpaceTimeKey(1,1,y2k.plusMillis(1)) )
                                  )
     idx.length should be (1)
     idx(0)._1 should be (0)
     idx(0)._2 should be (1)
      
  
    }
  }
}
