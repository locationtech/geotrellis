package geotrellis.vector.affine

import geotrellis.vector.MultiPoint

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait MultiPointTransformationMethods extends Any {
  val geom: MultiPoint

  def transform(trans: AffineTransformation): MultiPoint = {
    val geomCopy = trans.transform[jts.MultiPoint](geom.jtsGeom)
    MultiPoint(geomCopy)
  }

  def reflect(x: Double, y: Double): MultiPoint = {
    val trans = reflectionInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPoint = {
    val trans = reflectionInstance(x0, y0, x1, y1)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }
  
  def rotate(theta: Double): MultiPoint = {
    val trans = rotationInstance(theta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }

  def rotate(sinTheta: Double, cosTheta: Double): MultiPoint = {
    val trans = rotationInstance(sinTheta, cosTheta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }

  def scale(xscale: Double, yscale: Double): MultiPoint = {
    val trans = scaleInstance(xscale, yscale)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }

  def shear(xshear: Double, yshear: Double): MultiPoint = {
    val trans = shearInstance(xshear, yshear)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }

  def translate(x: Double, y: Double): MultiPoint = {
    val trans = translationInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPoint]
    MultiPoint(geomCopy)
  }
}
