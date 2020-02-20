/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.testkit

import geotrellis.raster.{CellGrid, Raster}
import geotrellis.vector.Extent

import scala.util.Try

object Utils {
  def roundRaster[T <: CellGrid[Int]](raster: Raster[T], scale: Int = 11): Raster[T] =
    raster.copy(extent = roundExtent(raster.extent, scale))

  def roundExtent(extent: Extent, scale: Int = 11): Extent = {
    val Extent(xmin, ymin, xmax, ymax) = extent
    Extent(
      BigDecimal(xmin).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble,
      BigDecimal(ymin).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble,
      BigDecimal(xmax).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble,
      BigDecimal(ymax).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble
    )
  }

  /** A dirty reflection function to modify object vals */
  def modifyField(obj: AnyRef, name: String, value: Any) {
    def impl(clazz: Class[_]) {
      Try(clazz.getDeclaredField(name)).toOption match {
        case Some(field) =>
          field.setAccessible(true)
          clazz.getMethod(name).invoke(obj) // force init in case it's a lazy val
          field.set(obj, value) // overwrite value
        case None =>
          if (clazz.getSuperclass != null) {
            impl(clazz.getSuperclass)
          }
      }
    }

    impl(obj.getClass)
  }
}
