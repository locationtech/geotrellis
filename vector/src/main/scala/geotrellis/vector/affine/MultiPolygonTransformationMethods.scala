package geotrellis.vector.affine

import geotrellis.util.MethodExtensions
import geotrellis.vector._

/** Trait used to implicitly extend [[Polygon]] instances with transformation methods */
trait MultiPolygonTransformationMethods extends MethodExtensions[MultiPolygon] {

  /**
    * Transform according to a provided AffineTransformation instance.
    *
    * @param  trans  An AffineTransformation
    */
  def transform(trans: AffineTransformation) = trans.transform(self)

  /** Reflect over the provided point */
  def reflect(p: Point): MultiPolygon =
    reflect(p.x, p.y)

  /** Reflect over the provided point */
  def reflect(x: Double, y: Double): MultiPolygon =
    Reflection(x, y).transform(self)

  /** Reflect over the line between two points */
  def reflect(p0: Point, p1: Point): MultiPolygon =
    reflect(p0.x, p0.y, p1.x, p1.y)

  /** Reflect over the line between two points */
  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPolygon =
    Reflection(x0, y0, x1, y1).transform(self)

  /** Rotate about its origin by the specified radians
    * @param theta  The number of radians about the origin to rotate this multipolygon. Positive
    *               numbers correspond to counter-clockwise rotation
    */
  def rotate(theta: Double): MultiPolygon =
    Rotation(theta).transform(self)

  /** Rotate about its origin by the specified angle
    * @param sinTheta  The sin of the angle angle about the origin to rotate this multipolygon
    * @param cosTheta  The cosin of the angle about the origin to rotate this multipolygon
    */
  def rotate(sinTheta: Double, cosTheta: Double): MultiPolygon =
    Rotation(sinTheta, cosTheta).transform(self)

  /** Change the scale of this multipolygon
    * @param xscale  the updated scale on the x-axis
    * @param yscale  the updated scale on the y-axis
    */
  def scale(xscale: Double, yscale: Double): MultiPolygon =
    Scaling(xscale, yscale).transform(self)

  /** Shear the provided polygon
    * @param xshear  Shear on the x axis
    * @param yshear  Shear on the y axis
    */
  def shear(xshear: Double, yshear: Double): MultiPolygon =
    Shearing(xshear, yshear).transform(self)

  /** Translate the provided polygon
    * @param x  the value to translate by in the x direction
    * @param y  the value to translate by in the y direction
    */
  def translate(x: Double, y: Double): MultiPolygon =
    Translation(x, y).transform(self)
}
