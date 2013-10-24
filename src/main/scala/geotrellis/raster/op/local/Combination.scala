package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import scala.collection.mutable

import scalaxy.loops._

// object Combination extends Serializable {
//   /** Assigns a unique but arbitrary identifier to each cell
//       exhibiting the combination across the each of the rasters
//       that is represented by that identifier. */
//   def apply(rs:Op[Raster]*)(implicit d:DI):Op[Raster] =
//     apply(rs)

//   /** Assigns a unique but arbitrary identifier to each cell
//       exhibiting the combination across the each of the rasters
//       that is represented by that identifier. */
//   def apply(rs:Seq[Op[Raster]]):Op[Raster] =
//     logic.Collect(rs).map { rs =>
//       if(Set(rs.map(_.rasterExtent)).size != 1) {
//         val rasterExtents = rs.map(_.rasterExtent).toSeq
//         throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
//           s"$rasterExtents are not all equal")
//       }

//       val layerCount = rs.length
//       if(layerCount == 0) {
//         sys.error(s"Can't compute mean of empty sequence")
//       } else {
//         val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
//         val re = rs(0).rasterExtent
//         val cols = re.cols
//         val rows = re.rows
//         val data = RasterData.allocByType(newRasterType,cols,rows)

//         if(newRasterType.isDouble) {
//           val combos = mutable.ListBuffer[Array[Double]]()
//           var combosLength = 0
          
//           // Used to compare combinations. Assumes same length
//           def compareArray(a1:Array[Double],a2:Array[Double]):Boolean = {
//             var i = 0
//             while(i < layerCount) { if(a1(i) != a2(i)) return false ; i += 1 }
//             return true
//           }

//           val stagingArray = Array.ofDim[Double](layerCount)

//           for(col <- 0 until cols optimized) {
//             for(row <- 0 until rows optimized) {
//               for(i <- 0 until layerCount optimized) { 
//                 stagingArray(i) = rs(i).get(col,row) 
//               }
//               var c = 0
//               var found = false
//               while(c < combosLength && !found) {
//                 if(compareArray(stagingArray,combos(c))) {
//                   found = true
//                   data.set(col,row,c)
//                 }
//                 c += 1
//               }
//               if(!found) {
//                 data.set(col,row,c)
//                 combos += stagingArray.clone
//                 combosLength += 1
//               }
//             }
//           }
//         } else {
//           val combos = mutable.ListBuffer[Array[Int]]()
//           var combosLength = 0
          
//           // Used to compare combinations. Assumes same length
//           def compareArray(a1:Array[Int],a2:Array[Int]):Boolean = {
//             var i = 0
//             while(i < layerCount) { if(a1(i) != a2(i)) return false ; i += 1 }
//             return true
//           }

//           val stagingArray = Array.ofDim[Int](layerCount)

//           for(col <- 0 until cols optimized) {
//             for(row <- 0 until rows optimized) {
//               for(i <- 0 until layerCount optimized) { 
//                 stagingArray(i) = rs(i).get(col,row) 
//               }
//               var c = 0
//               var found = false
//               while(c < combosLength && !found) {
//                 if(compareArray(stagingArray,combos(c))) {
//                   found = true
//                   data.set(col,row,c)
//                 }
//                 c += 1
//               }
//               if(!found) {
//                 data.set(col,row,c)
//                 combos += stagingArray.clone
//                 combosLength += 1
//               }
//             }
//           }
//         }
//         Raster(data,re)
//       }
//     }
//     .withName("Combination")
// }
