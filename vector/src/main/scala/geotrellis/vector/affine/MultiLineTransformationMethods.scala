package geotrellis.vector.affine

import geotrellis.vector.MultiLine

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait MultiLineTransformationMethods extends Any {
  val geom: MultiLine

  def transform(trans: AffineTransformation) = trans.transform(geom)

  def reflect(x: Double, y: Double): MultiLine = 
    Reflection(x, y).transform(geom)

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiLine = 
    Reflection(x0, y0, x1, y1).transform(geom)
  
  def rotate(theta: Double): MultiLine = 
    Rotation(theta).transform(geom)

  def rotate(sinTheta: Double, cosTheta: Double): MultiLine = 
    Rotation(sinTheta, cosTheta).transform(geom)

  def scale(xscale: Double, yscale: Double): MultiLine = 
    Scaling(xscale, yscale).transform(geom)

  def shear(xshear: Double, yshear: Double): MultiLine = 
    Shearing(xshear, yshear).transform(geom)

  def translate(x: Double, y: Double): MultiLine = 
    Translation(x, y).transform(geom)
}
