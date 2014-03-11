/***
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
 ***/

package geotrellis.render.png

sealed abstract class ColorType(val n:Byte, val depth:Int)

// greyscale and color opaque rasters
case class Grey(transparent:Int) extends ColorType(0, 1)
case class Rgb(transparent:Int) extends ColorType(2, 3)

// indexed color, using separate rgb and alpha channels
case class Indexed(rgbs:Array[Int], as:Array[Int]) extends ColorType(3, 1)

// greyscale and color rasters with an alpha byte
case object Greya extends ColorType(4, 4)
case object Rgba extends ColorType(6, 4)
