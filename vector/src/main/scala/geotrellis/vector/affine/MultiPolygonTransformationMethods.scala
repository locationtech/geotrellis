package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector.MultiPolygon

trait MultiPolygonTransformationMethods extends MethodExtensions[MultiPolygon] {
  def transform(trans: AffineTransformation) = trans.transform(self)

  def reflect(x: Double, y: Double): MultiPolygon =
    Reflection(x, y).transform(self)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPolygon =
    Reflection(x0, y0, x1, y1).transform(self)

  def rotate(theta: Double): MultiPolygon =
    Rotation(theta).transform(self)

  def rotate(sinTheta: Double, cosTheta: Double): MultiPolygon =
    Rotation(sinTheta, cosTheta).transform(self)

  def scale(xscale: Double, yscale: Double): MultiPolygon =
    Scaling(xscale, yscale).transform(self)

  def shear(xshear: Double, yshear: Double): MultiPolygon =
    Shearing(xshear, yshear).transform(self)

  def translate(x: Double, y: Double): MultiPolygon =
    Translation(x, y).transform(self)
}
