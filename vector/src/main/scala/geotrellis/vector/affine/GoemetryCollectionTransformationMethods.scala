package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector._

trait GeometryCollectionTransformationMethods extends MethodExtensions[GeometryCollection] {
  def transform(trans: AffineTransformation) = trans.transform(self)

  def reflect(x: Double, y: Double): GeometryCollection =
    Reflection(x, y).transform(self)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): GeometryCollection =
    Reflection(x0, y0, x1, y1).transform(self)

  def rotate(theta: Double): GeometryCollection =
    Rotation(theta).transform(self)

  def rotate(sinTheta: Double, cosTheta: Double): GeometryCollection =
    Rotation(sinTheta, cosTheta).transform(self)

  def scale(xscale: Double, yscale: Double): GeometryCollection =
    Scaling(xscale, yscale).transform(self)

  def shear(xshear: Double, yshear: Double): GeometryCollection =
    Shearing(xshear, yshear).transform(self)

  def translate(x: Double, y: Double): GeometryCollection =
    Translation(x, y).transform(self)
}
