package geotrellis.spark.io.index.zcurve

import org.scalatest._
import geotrellis.spark.SpaceTimeKey
import org.joda.time.DateTime
import scala.collection.immutable.TreeSet

class ZSpaceTimeKeySpec extends FunSpec with Matchers{
   val y2k = new DateTime(2000, 1, 1, 0, 0)
   val UpperBound = 8

  describe("ZSpaceTimeKey test"){

    it("indexes time"){
     val zst = new ZSpaceTimeKeyIndex( { dt => dt.getYear } )
     var i = 0                  
     var j = 0                  
     var k = 0
     var ts: TreeSet[Long] = TreeSet()
                                
     while (i < UpperBound) {  
      j=0
        while(j < UpperBound){
         k=0
           while(k < UpperBound){
              var dt = new DateTime(2000+k, 1,1,0,0)
              var idx = zst.toIndex(SpaceTimeKey(j,i,dt))
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

    it("generates indexes you can check by hand 2x2x2"){
     val zst = ZSpaceTimeKeyIndex( {dt => dt.getMillis.toInt } )
     var idx = List[SpaceTimeKey](
                                  SpaceTimeKey(0,0, y2k),
                                  SpaceTimeKey(1,0, y2k),
                                  SpaceTimeKey(0,1, y2k),
                                  SpaceTimeKey(1,1, y2k),

                                  SpaceTimeKey(0,0, y2k.plusMillis(1)),
                                  SpaceTimeKey(1,0, y2k.plusMillis(1)),
                                  SpaceTimeKey(0,1, y2k.plusMillis(1)),
                                  SpaceTimeKey(1,1, y2k.plusMillis(1))
                                 )
     for { i <- 0 to 6 }
	zst.toIndex(idx(i)) should be (zst.toIndex(idx(i+1)) - 1)

     zst.toIndex(idx(6)) should be (zst.toIndex(idx(7)) - 1)
    }

    it("generates a Seq[(Long,Long)] from a keyRange: (SpaceTimeKey, SpaceTimeKey)"){
      val zst = ZSpaceTimeKeyIndex( {dt => dt.getMillis.toInt} )

      //all sub cubes in a 2x2x2
      var idx = zst.indexRanges( (SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (7) 
    
      //sub cubes along x
      idx = zst.indexRanges( (SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,0,y2k.plusMillis(1))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1) 
      (idx(1)._2 - idx(1)._1) should be (1) 
    
      //next sub cubes along x
      idx = zst.indexRanges( (SpaceTimeKey(0,1,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1) 
      (idx(1)._2 - idx(1)._1) should be (1) 
    
      //sub cubes along y
      idx = zst.indexRanges( (SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,1,y2k.plusMillis(1))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0) 
      (idx(1)._2 - idx(1)._1) should be (0) 
      (idx(2)._2 - idx(2)._1) should be (0) 
      (idx(3)._2 - idx(3)._1) should be (0) 
    
      //next sub cubes along y
      idx = zst.indexRanges( (SpaceTimeKey(1,0,y2k), SpaceTimeKey(1,1,y2k.plusMillis(1))))
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
      idx = zst.indexRanges( (SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k)) )
      idx.length should be (1)
     (idx(0)._2 - idx(0)._1) should be (3) 
    }
  }
}
