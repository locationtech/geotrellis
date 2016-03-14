package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector.Polygon

trait PolygonTransformationMethods extends MethodExtensions[Polygon] {
  def transform(trans: AffineTransformation) = trans.transform(self)

  def reflect(x: Double, y: Double): Polygon =
    Reflection(x, y).transform(self)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Polygon =
    Reflection(x0, y0, x1, y1).transform(self)

  def rotate(theta: Double): Polygon =
    Rotation(theta).transform(self)

  def rotate(sinTheta: Double, cosTheta: Double): Polygon =
    Rotation(sinTheta, cosTheta).transform(self)

  def scale(xscale: Double, yscale: Double): Polygon =
    Scaling(xscale, yscale).transform(self)

  def shear(xshear: Double, yshear: Double): Polygon =
    Shearing(xshear, yshear).transform(self)

  def translate(x: Double, y: Double): Polygon =
    Translation(x, y).transform(self)
}
