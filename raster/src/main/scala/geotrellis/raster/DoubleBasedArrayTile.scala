/*
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
 */

package geotrellis.raster

import geotrellis._

/**
 * This trait defines apply/update in terms of applyDouble/updateDouble.
 */
trait DoubleBasedArrayTile {
  def apply(i:Int):Int = d2i(applyDouble(i))
  def update(i:Int, z:Int):Unit = updateDouble(i, i2d(z))

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}

trait RawDoubleBasedArray {
  def apply(i:Int):Int = applyDouble(i).toInt
  def update(i:Int, z:Int):Unit = updateDouble(i, z.toInt)

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}

trait DynamicDoubleBasedArray {
  val noDataValue: Double

  def apply(i:Int):Int = {
    val n = applyDouble(i)
    if (n == noDataValue.toDouble) NODATA else n.toInt
  }

  def update(i:Int, z:Int):Unit = updateDouble(i, if (z == noDataValue.toInt) NODATA else z)

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}
