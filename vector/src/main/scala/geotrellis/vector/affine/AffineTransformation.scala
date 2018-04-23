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

package geotrellis.vector.affine

import geotrellis.vector._
import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.geom.{util => jtsutil}

object AffineTransformation {
  def apply(): AffineTransformation =
    apply(new jtsutil.AffineTransformation)

  def apply(jtsTrans: jtsutil.AffineTransformation): AffineTransformation =
    new AffineTransformation { val trans = jtsTrans }
}

trait AffineTransformation extends Serializable {
  val trans: jtsutil.AffineTransformation

  def transform(geom: Point): Point = Point(transform(geom.jtsGeom))
  def transform(geom: Line): Line = Line(transform(geom.jtsGeom))
  def transform(geom: Polygon): Polygon = Polygon(transform(geom.jtsGeom))
  def transform(geom: MultiPoint): MultiPoint = MultiPoint(transform(geom.jtsGeom))
  def transform(geom: MultiLine): MultiLine = MultiLine(transform(geom.jtsGeom))
  def transform(geom: MultiPolygon): MultiPolygon = MultiPolygon(transform(geom.jtsGeom))
  def transform(geom: GeometryCollection): GeometryCollection = GeometryCollection(transform(geom.jtsGeom))
  def transform(geom: Geometry): Geometry = Geometry(transform(geom.jtsGeom))

  private def transform[D <: jts.Geometry](g: D): D = trans.transform(g).asInstanceOf[D]

  def reflect(x: Double, y: Double): AffineTransformation = AffineTransformation(trans.reflect(x, y))

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation = AffineTransformation(trans.reflect(x0, y0, x1, y1))

  def rotate(theta: Double): AffineTransformation = AffineTransformation(trans.rotate(theta))
  def rotate(sinTheta: Double, cosTheta: Double): AffineTransformation = AffineTransformation(trans.rotate(sinTheta, cosTheta))

  def scale(xscale: Double, yscale: Double): AffineTransformation = AffineTransformation(trans.scale(xscale, yscale))

  def shear(xshear: Double, yshear: Double): AffineTransformation = AffineTransformation(trans.shear(xshear, yshear))

  def translate(x: Double, y: Double): AffineTransformation = AffineTransformation(trans.translate(x, y))

}

object Reflection {
  def apply(x: Double, y: Double): AffineTransformation =
    AffineTransformation().reflect(x, y)

  def apply(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation =
    AffineTransformation().reflect(x0, y0, x1, y1)
}

object Rotation {
  def apply(theta: Double): AffineTransformation =
    AffineTransformation().rotate(theta)

  def apply(sinTheta: Double, cosTheta: Double): AffineTransformation =
    AffineTransformation().rotate(sinTheta, cosTheta)
}

object Scaling {
  def apply(xscale: Double, yscale: Double): AffineTransformation =
    AffineTransformation().scale(xscale, yscale)
}

object Shearing {
  def apply(xshear: Double, yshear: Double): AffineTransformation =
    AffineTransformation().shear(xshear, yshear)
}

object Translation {
  def apply(x: Double, y: Double): AffineTransformation =
    AffineTransformation().translate(x, y)
}
