package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector._

/** Trait used to implicitly extend [[Line]] instances with transformation methods */
trait LineTransformationMethods extends MethodExtensions[Line] {

  /**
    * Transform according to a provided AffineTransformation instance.
    *
    * @param  trans  An AffineTransformation
    */
  def transform(trans: AffineTransformation) = trans.transform(self)

  /** Reflect over the provided point */
  def reflect(p: Point): Line =
    reflect(p.x, p.y)

  /** Reflect over the provided point */
  def reflect(x: Double, y: Double): Line =
    Reflection(x, y).transform(self)

  /** Reflect over the line between two points */
  def reflect(p0: Point, p1: Point): Line =
    reflect(p0.x, p0.y, p1.x, p1.y)

  /** Reflect over the line between two points */
  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Line =
    Reflection(x0, y0, x1, y1).transform(self)

  /** Rotate about its origin by the specified radians
    * @param theta  The number of radians about the origin to rotate this line. Positive
    *               numbers correspond to counter-clockwise rotation
    */
  def rotate(theta: Double): Line =
    Rotation(theta).transform(self)

  /** Rotate about its origin by the specified angle
    * @param sinTheta  The sin of the angle angle about the origin to rotate this line
    * @param cosTheta  The cosin of the angle about the origin to rotate this line
    */
  def rotate(sinTheta: Double, cosTheta: Double): Line =
    Rotation(sinTheta, cosTheta).transform(self)

  /** Change the scale of this line
    * @param xscale  the updated scale on the x-axis
    * @param yscale  the updated scale on the y-axis
    */
  def scale(xscale: Double, yscale: Double): Line =
    Scaling(xscale, yscale).transform(self)

  /** Shear the provided line
    * @param xshear  Shear on the x axis
    * @param yshear  Shear on the y axis
    */
  def shear(xshear: Double, yshear: Double): Line =
    Shearing(xshear, yshear).transform(self)

  /** Translate the provided line
    * @param x  the value to translate by in the x direction
    * @param y  the value to translate by in the y direction
    */
  def translate(x: Double, y: Double): Line =
    Translation(x, y).transform(self)
}
