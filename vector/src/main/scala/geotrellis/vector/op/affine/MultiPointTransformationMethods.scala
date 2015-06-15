package geotrellis.vector.op.affine

import geotrellis.vector.MultiPoint

trait MultiPointTransformationMethods {
  val geom: MultiPoint

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): MultiPoint = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPoint = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): MultiPoint = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): MultiPoint = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): MultiPoint = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): MultiPoint = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): MultiPoint = 
    Translation(x, y).transform(geom)
}
