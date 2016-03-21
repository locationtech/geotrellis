package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector.Line

trait LineTransformationMethods extends MethodExtensions[Line] {
  def transform(trans: AffineTransformation) = trans.transform(self)

  def reflect(x: Double, y: Double): Line =
    Reflection(x, y).transform(self)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Line =
    Reflection(x0, y0, x1, y1).transform(self)

  def rotate(theta: Double): Line =
    Rotation(theta).transform(self)

  def rotate(sinTheta: Double, cosTheta: Double): Line =
    Rotation(sinTheta, cosTheta).transform(self)

  def scale(xscale: Double, yscale: Double): Line =
    Scaling(xscale, yscale).transform(self)

  def shear(xshear: Double, yshear: Double): Line =
    Shearing(xshear, yshear).transform(self)

  def translate(x: Double, y: Double): Line =
    Translation(x, y).transform(self)
}
