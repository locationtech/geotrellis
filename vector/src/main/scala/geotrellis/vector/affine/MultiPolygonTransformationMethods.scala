package geotrellis.vector.affine

import geotrellis.vector.MultiPolygon

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait MultiPolygonTransformationMethods extends Any {
  val geom: MultiPolygon

  def transform(trans: AffineTransformation): MultiPolygon = {
    val geomCopy = trans.transform[jts.MultiPolygon](geom.jtsGeom)
    MultiPolygon(geomCopy)
  }

  def reflect(x: Double, y: Double): MultiPolygon = {
    val trans = reflectionInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): MultiPolygon = {
    val trans = reflectionInstance(x0, y0, x1, y1)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }
  
  def rotate(theta: Double): MultiPolygon = {
    val trans = rotationInstance(theta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }

  def rotate(sinTheta: Double, cosTheta: Double): MultiPolygon = {
    val trans = rotationInstance(sinTheta, cosTheta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }

  def scale(xscale: Double, yscale: Double): MultiPolygon = {
    val trans = scaleInstance(xscale, yscale)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }

  def shear(xshear: Double, yshear: Double): MultiPolygon = {
    val trans = shearInstance(xshear, yshear)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }

  def translate(x: Double, y: Double): MultiPolygon = {
    val trans = translationInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.MultiPolygon]
    MultiPolygon(geomCopy)
  }
}

