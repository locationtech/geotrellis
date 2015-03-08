package geotrellis.vector.affine

import geotrellis.vector.Polygon

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.util.AffineTransformation._


trait PolygonTransformationMethods extends Any {
  val geom: Polygon

  def transform(trans: AffineTransformation): Polygon = {
    val geomCopy = trans.transform[jts.Polygon](geom.jtsGeom)
    Polygon(geomCopy)
  }

  def reflect(x: Double, y: Double): Polygon = {
    val trans = reflectionInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): Polygon = {
    val trans = reflectionInstance(x0, y0, x1, y1)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }
  
  def rotate(theta: Double): Polygon = {
    val trans = rotationInstance(theta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }

  def rotate(sinTheta: Double, cosTheta: Double): Polygon = {
    val trans = rotationInstance(sinTheta, cosTheta)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }

  def scale(xscale: Double, yscale: Double): Polygon = {
    val trans = scaleInstance(xscale, yscale)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }

  def shear(xshear: Double, yshear: Double): Polygon = {
    val trans = shearInstance(xshear, yshear)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }

  def translate(x: Double, y: Double): Polygon = {
    val trans = translationInstance(x, y)
    val geomCopy = trans.transform(geom.jtsGeom).asInstanceOf[jts.Polygon]
    Polygon(geomCopy)
  }
}
