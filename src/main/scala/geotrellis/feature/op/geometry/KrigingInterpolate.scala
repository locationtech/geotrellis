package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import geotrellis.raster._

import scalaxy.loops._

abstract sealed class KrigingMethod
// Enumeration in Scala
case object Ordinary extends KrigingMethod
case object Simple extends KrigingMethod
case object Universeal extends KrigingMethod

/* where n is the number of scatter points in the set,
   fi are the values of the scatter points, and wi are
  weights assigned to each scatter point. This equation
  is essentially the same as the equation used for inverse
  distance weighted interpolation (equation 9.8) except
  that rather than using weights based on an arbitrary
  function of distance, the weights used in kriging are
  based on the model variogram. For example, to interpolate
  at a point P based on the surrounding points P1, P2, and
  P3, the weights w1, w2, and w3 must be found. The weights
  are found through the solution of the simultaneous equations:
  
  F(p0) = Σ(wi*fi)
  
  w1*SV(d11) + w2*SV(d12) + w3*SV(d13) ... + λ = SV(d1p)
  w1*SV(d12) + w2*SV(d22) + w3*SV(d32) ... + λ = SV(d2p)
  w1*SV(d13) + w2*SV(d23) + w3*SV(d33) ... + λ = SV(d3p)
  ...
  w1 + w2 + w3 + 0 = 1.0

  fp = wf1 + wf2 + wf3 + ...
*/

// object KrigingInterpolate {
//   def getWeight(pts:Seq[Point[Int]],dest:Point[Int]) = {
//     val sv:Function1[Double,Double] = Variogram(pts)
//     length = pts.length
//     for (i <- 0 until length optimized) {
//       // eq1 coefficients
//       pts.map{ a => GetDistance() }
//     }


//   }
// }

// /**
//  * Kriging Interpolation
//  */
// case class KrigingInterpolate(points:Seq[Point[Int]],re:RasterExtent,radius:Option[Int]=None,method:KrigingMethod) {
//     val cols = re.cols
//     val rows = re.rows
//     val data = RasterData.emptyByType(TypeInt, cols, rows)

//     val index:SpatialIndex[Point[Int]] = SpatialIndex(points)(p => (p.x,p.y))

//     if(points.isEmpty) {
//       Result(Raster(data,re))
//     } else {
//       radius match {
//         case Some(r) =>
//           for(col <- 0 until cols optimized) {
//             for(row <- 0 until rows optimized) {
//               val destX = re.gridColToMap(col)
//               val destY = re.gridRowToMap(row)
//               val dest = Point(destX,destY)
//               val pts = index.
//                 pointsInExtent(Extent(destX - r, destY - r, destX + r, destY + r))
//                   .map( a => GetDistance(dest,a) < r )

//               getWeight(pts,dest)

//               val length = pts.length
//               for(i <- 0 until length optimized) {
//                 val point = pts(i)
//                 val dX = (destX - point.x)
//                 val dY = (destY - point.y)
//                 val d = sqrt(dX * dX + dY * dY)
//                 if (d < r) {
//                   val w = 1 / d
//                   s += point.data * w
//                   ws += w
//                   c += 1
//                 }
//               }
//               if (c == 0) {
//                 data.set(col, row, NODATA)
//               } else {
//                 val mean = s / ws
//                 data.set(col, row, mean.toInt)
//               }
//             }
    
//     Result(Raster(data,re))
// })
