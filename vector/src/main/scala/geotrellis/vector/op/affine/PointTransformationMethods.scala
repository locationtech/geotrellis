package geotrellis.vector.op.affine

import geotrellis.vector.Point

trait PointTransformationMethods {
  val geom: Point

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): Point = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Point = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): Point = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): Point = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): Point = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): Point = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): Point = 
    Translation(x, y).transform(geom)

}
