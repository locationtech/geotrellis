/*
 * Copyright 2016 Azavea
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

package geotrellis.proj4

import org.osgeo.proj4j._

object Transform {
  def apply(src: CRS, dest: CRS): (Double, Double) => (Double, Double) =
    src.alternateTransform(dest) match {
      case Some(f) => f
      case None => Proj4Transform(src, dest)
    }
}

object Proj4Transform {
  def apply(src: CRS, dest: CRS): Transform = {
    val t = new BasicCoordinateTransform(src.proj4jCrs, dest.proj4jCrs)

    { (x: Double, y: Double) =>
      val srcP = new ProjCoordinate(x, y)
      val destP = new ProjCoordinate
      t.transform(srcP, destP)
      (destP.x, destP.y)
    }
  }
}
