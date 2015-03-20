package geotrellis.vector.affine

import geotrellis.vector.Line

trait LineTransformationMethods extends Any {
  val geom: Line

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): Line = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Line = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): Line = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): Line = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): Line = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): Line = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): Line = 
    Translation(x, y).transform(geom)
}
