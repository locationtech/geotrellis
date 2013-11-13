package geotrellis.statistics.op.stat

import scala.math.max

import geotrellis._

// case class Max(r:Op[Raster]) extends logic.TileReducer1[Int] {
//   type B = Int

//   val maxOp = op { 
//     r:Raster => {
//       var zmax = Int.MinValue
//       r.foreach(z => if (z != NODATA) zmax = max(z, zmax))
//       zmax
//     }
//   }

//   def mapper(r:Op[Raster]) = logic.AsList(maxOp(r))
//   def reducer(results:List[Int]) =  results.reduceLeft(max)
// }
