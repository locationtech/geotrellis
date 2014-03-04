/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._

import com.vividsolutions.jts.operation.buffer.{BufferParameters,BufferOp}

sealed abstract class EndCapStyle { def value:Int }
case object EndCapRound extends EndCapStyle { def value = 1 }
case object EndCapFlat extends EndCapStyle { def value = 2 }
case object EndCapSquare extends EndCapStyle { def value = 3} 

/**
  * Computes a buffer area around this geometry.
  *
  * @param g  Geometry to buffer
  * @param distance  Distance to buffer
  * @param detail  Number of line segments used to approximate a quarter circle (default 8) 
  * @param endCapStyle  Specify flat, round, or square geometry at the end of lines
  *
  * @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#buffer(double,int,int) "JTS documentation"]]
  */
case class Buffer[A](g:Op[Geometry[A]], distance:Op[Double], detail:Op[Int], endCapStyle:EndCapStyle) extends Op3(g,distance,detail)  ({
  (g,distance,detail) => Result(Feature(g.geom.buffer(distance, detail, endCapStyle.value),g.data))
})

object Buffer {
 /**
  * Computes a buffer area around this geometry.
  *
  * @param g  Geometry to buffer
  * @param distance  Distance to buffer
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#buffer(double) "JTS documentation"]]
  */
 def apply[D:Manifest](g:Op[Geometry[D]], distance:Op[Double]):Op[Geometry[D]] = Buffer(g, distance, 8, EndCapRound)
}

/**
 * Computes a buffer area around with geometry based on specified buffer parameters.
 *
 * @param g  Geometry to buffer
 * @param distance  Distance to buffer
 * @param params BufferParameters object 
 *
 * @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/operation/buffer/BufferParameters.html "JTS BufferParameters documentation"]]
 */
case class BufferWithParameters[A] (f:Op[Geometry[A]], distance:Op[Double], params:BufferParameters) extends Op3(f,distance,params) ({
  (g,distance, params) => Result(Feature(BufferOp.bufferOp(g.geom, distance, params), g.data))
}) 
