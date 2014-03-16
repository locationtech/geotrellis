/**************************************************************************
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.process._
import scala.math.{max,min}

/**
  * Returns a tuple with the minimum and maximum value of a raster.
  *
  * @note    Currently does not support finding double Min and Max values.
  *          If used with a raster with a double data type (TypeFloat,TypeDouble),
  *          will find the integer min and max of the rounded cell values.
  */
// case class GetMinMax(r:Op[Raster]) extends logic.TileReducer1[(Int,Int)] {
//   type B = (Int,Int)

//   val maxOp = op {
//     (r:Raster) => Result(r.findMinMax) 
//   }

//   def mapper(r:Op[Raster]) = logic.AsList(maxOp(r))
//   def reducer(results:List[(Int,Int)]) = results.reduceLeft( (a,b) => (min(a._1, b._1), max(a._2, b._2) )) 
// }
