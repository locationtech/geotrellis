package geotrellis.vector.affine

import geotrellis.vector.Point

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait PointTransformationMethods extends Any {
  val geom: Point

  def transform(trans: AffineTransformation): Point = {
    val geomCopy = trans.transform[jts.Point](geom.jtsGeom)
    Point(geomCopy)
  }

  def reflect(x: Double, y: Double): Point = {
    val trans = reflectionInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Point = {
    val trans = reflectionInstance(x0, y0, x1, y1)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }
  
  def rotate(theta: Double): Point = {
    val trans = rotationInstance(theta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }

  def rotate(sinTheta: Double, cosTheta: Double): Point = {
    val trans = rotationInstance(sinTheta, cosTheta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }

  def scale(xscale: Double, yscale: Double): Point = {
    val trans = scaleInstance(xscale, yscale)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }

  def shear(xshear: Double, yshear: Double): Point = {
    val trans = shearInstance(xshear, yshear)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }

  def translate(x: Double, y: Double): Point = {
    val trans = translationInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Point]
    Point(geomCopy)
  }
}

