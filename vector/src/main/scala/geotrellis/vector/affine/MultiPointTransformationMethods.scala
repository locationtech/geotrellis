package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector.MultiPoint

trait MultiPointTransformationMethods extends MethodExtensions[MultiPoint] {
  def transform(trans: AffineTransformation) = trans.transform(self)

  def reflect(x: Double, y: Double): MultiPoint =
    Reflection(x, y).transform(self)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPoint =
    Reflection(x0, y0, x1, y1).transform(self)

  def rotate(theta: Double): MultiPoint =
    Rotation(theta).transform(self)

  def rotate(sinTheta: Double, cosTheta: Double): MultiPoint =
    Rotation(sinTheta, cosTheta).transform(self)

  def scale(xscale: Double, yscale: Double): MultiPoint =
    Scaling(xscale, yscale).transform(self)

  def shear(xshear: Double, yshear: Double): MultiPoint =
    Shearing(xshear, yshear).transform(self)

  def translate(x: Double, y: Double): MultiPoint =
    Translation(x, y).transform(self)
}
