package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector._

/** Trait used to implicitly extend [[Polygon]] instances with transformation methods */
trait PolygonTransformationMethods extends MethodExtensions[Polygon] {

  /**
    * Transform according to a provided AffineTransformation instance.
    *
    * @param  trans  An AffineTransformation
    */
  def transform(trans: AffineTransformation) = trans.transform(self)

  /** Reflect over the provided point */
  def reflect(p: Point): Polygon =
    reflect(p.x, p.y)

  /** Reflect over the provided point */
  def reflect(x: Double, y: Double): Polygon =
    Reflection(x, y).transform(self)

  /** Reflect over the line between two points */
  def reflect(p0: Point, p1: Point): Polygon =
    reflect(p0.x, p0.y, p1.x, p1.y)

  /** Reflect over the line between two points */
  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Polygon =
    Reflection(x0, y0, x1, y1).transform(self)

  /** Rotate about its origin by the specified radians
    * @param theta  The number of radians about the origin to rotate this polygon. Positive
    *               numbers correspond to counter-clockwise rotation
    */
  def rotate(theta: Double): Polygon =
    Rotation(theta).transform(self)

  /** Rotate about its origin by the specified angle
    * @param sinTheta  The sin of the angle angle about the origin to rotate this polygon
    * @param cosTheta  The cosin of the angle about the origin to rotate this polygon
    */
  def rotate(sinTheta: Double, cosTheta: Double): Polygon =
    Rotation(sinTheta, cosTheta).transform(self)

  /** Change the scale of this polygon
    * @param xscale  the updated scale on the x-axis
    * @param yscale  the updated scale on the y-axis
    */
  def scale(xscale: Double, yscale: Double): Polygon =
    Scaling(xscale, yscale).transform(self)

  /** Shear the provided polygon
    * @param xshear  Shear on the x axis
    * @param yshear  Shear on the y axis
    */
  def shear(xshear: Double, yshear: Double): Polygon =
    Shearing(xshear, yshear).transform(self)

  /** Translate the provided polygon
    * @param x  the value to translate by in the x direction
    * @param y  the value to translate by in the y direction
    */
  def translate(x: Double, y: Double): Polygon =
    Translation(x, y).transform(self)
}
