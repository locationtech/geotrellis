package geotrellis.vector.affine

import geotrellis.vector._

trait GeometryCollectionTransformationMethods extends Any {
  val geom: GeometryCollection

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): GeometryCollection = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): GeometryCollection = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): GeometryCollection = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): GeometryCollection = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): GeometryCollection = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): GeometryCollection = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): GeometryCollection = 
    Translation(x, y).transform(geom)
}
