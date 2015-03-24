package geotrellis.vector.affine

import geotrellis.vector.Polygon

trait PolygonTransformationMethods extends Any {
  val geom: Polygon

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): Polygon = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Polygon = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): Polygon = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): Polygon = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): Polygon = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): Polygon = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): Polygon = 
    Translation(x, y).transform(geom)
}
