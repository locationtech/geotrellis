package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector.MultiLine

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait MultiLineTransformationMethods extends MethodExtensions[MultiLine] {
  def transform(trans: AffineTransformation) = trans.transform(self)

  def reflect(x: Double, y: Double): MultiLine =
    Reflection(x, y).transform(self)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiLine =
    Reflection(x0, y0, x1, y1).transform(self)

  def rotate(theta: Double): MultiLine =
    Rotation(theta).transform(self)

  def rotate(sinTheta: Double, cosTheta: Double): MultiLine =
    Rotation(sinTheta, cosTheta).transform(self)

  def scale(xscale: Double, yscale: Double): MultiLine =
    Scaling(xscale, yscale).transform(self)

  def shear(xshear: Double, yshear: Double): MultiLine =
    Shearing(xshear, yshear).transform(self)

  def translate(x: Double, y: Double): MultiLine =
    Translation(x, y).transform(self)
}
