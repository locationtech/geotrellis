package geotrellis.data

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._
import geotrellis.raster._

import scala.collection.mutable

// TODO
//@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
// class LazyCombineSpec extends FunSpec 
//                          with ShouldMatchers 
//                          with TestServer 
//                          with RasterBuilders {
//   describe("LazyCombine") {
//     it("should combine raster values with foreach") {
//       val data = createConsecutiveRaster(5,6).data.asArray
//       val lzc = LazyCombine(data,data,(z1,z2) => z1 + z2)
      
//       val expected = for(i <- 1 to 30) yield { i + i }      

//       val s = mutable.Set[Int]()

//       for(z <- lzc) { s += z }
      
//       s.toSeq.sorted should be (expected)
//     }

//     it("should give the correct array for asArray") {
//       val ones = createValueRaster(5,6,1).data.asArray
//       val consecutive1 = createConsecutiveRaster(5,6).data.asArray
//       val consecutive2 = createConsecutiveRaster(5,6,startingFrom=2).data.asArray

//       val lzc = LazyCombine(ones,consecutive1,(z1,z2) => z1 + z2)
      
//       lzc.asArray should be (consecutive2)
//     }

//     it("should compose with map") {
//       val ones = createValueRaster(5,6,1).data.asArray
//       val consecutive1 = createConsecutiveRaster(5,6).data.asArray
//       val consecutive3 = createConsecutiveRaster(5,6,startingFrom=3).data.asArray

//       val lzc = LazyCombine(ones,consecutive1,(z1,z2) => z1 + z2)
            
//       lzc.map(_ + 1).asArray should be (consecutive3)
//     }

//     it("should compose with mapIfSet and skip NoData returned from the combine function") {
//       val ones = replaceValues(createValueRaster(5,6,1), Map( (1,1) -> NODATA)).data.asArray
//       val consecutive1 = createConsecutiveRaster(5,6).data.asArray
//       val consecutive2 = 
//         replaceValues(createConsecutiveRaster(5,6,startingFrom=2),Map( (1,1) -> NODATA)).data.asArray

//       val lzc = LazyCombine(ones,consecutive1,(z1,z2) => if(z1 != NODATA && z2 != NODATA) {z1 + z2} else {NODATA})
//       lzc.asArray should be (consecutive2)
//     }
//   }
// }
