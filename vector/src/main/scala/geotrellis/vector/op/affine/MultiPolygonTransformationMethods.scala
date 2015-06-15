package geotrellis.vector.op.affine

import geotrellis.vector.MultiPolygon

trait MultiPolygonTransformationMethods {
  val geom: MultiPolygon

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): MultiPolygon = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPolygon = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): MultiPolygon = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): MultiPolygon = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): MultiPolygon = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): MultiPolygon = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): MultiPolygon = 
    Translation(x, y).transform(geom)
}
