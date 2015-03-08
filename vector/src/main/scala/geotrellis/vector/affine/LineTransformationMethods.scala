package geotrellis.vector.affine

import geotrellis.vector.Line

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait LineTransformationMethods extends Any {
  val geom: Line

  def transform(trans: AffineTransformation): Line = {
    val geomCopy = trans.transform[jts.LineString](geom.jtsGeom)
    Line(geomCopy)
  }

  def reflect(x: Double, y: Double): Line = {
    val trans = reflectionInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Line = {
    val trans = reflectionInstance(x0, y0, x1, y1)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }
  
  def rotate(theta: Double): Line = {
    val trans = rotationInstance(theta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }

  def rotate(sinTheta: Double, cosTheta: Double): Line = {
    val trans = rotationInstance(sinTheta, cosTheta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }

  def scale(xscale: Double, yscale: Double): Line = {
    val trans = scaleInstance(xscale, yscale)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }

  def shear(xshear: Double, yshear: Double): Line = {
    val trans = shearInstance(xshear, yshear)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }

  def translate(x: Double, y: Double): Line = {
    val trans = translationInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.LineString]
    Line(geomCopy)
  }
}
