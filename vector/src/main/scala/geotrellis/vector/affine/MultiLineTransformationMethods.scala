package geotrellis.vector.affine

import geotrellis.vector.MultiLine

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait MultiLineTransformationMethods extends Any {
  val geom: MultiLine

  def transform(trans: AffineTransformation): MultiLine = {
    val geomCopy = trans.transform[jts.MultiLineString](geom.jtsGeom)
    MultiLine(geomCopy)
  }

  def reflect(x: Double, y: Double): MultiLine = {
    val trans = reflectionInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiLine = {
    val trans = reflectionInstance(x0, y0, x1, y1)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }
  
  def rotate(theta: Double): MultiLine = {
    val trans = rotationInstance(theta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }

  def rotate(sinTheta: Double, cosTheta: Double): MultiLine = {
    val trans = rotationInstance(sinTheta, cosTheta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }

  def scale(xscale: Double, yscale: Double): MultiLine = {
    val trans = scaleInstance(xscale, yscale)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }

  def shear(xshear: Double, yshear: Double): MultiLine = {
    val trans = shearInstance(xshear, yshear)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }

  def translate(x: Double, y: Double): MultiLine = {
    val trans = translationInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiLineString]
    MultiLine(geomCopy)
  }
}
