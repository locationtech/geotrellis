package geotrellis.vector.affine

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.{util => jtsutil}


case class AffineTransformation(trans: jtsutil.AffineTransformation = new jtsutil.AffineTransformation()) {

  def transform[D <: jts.Geometry](g: D): D = trans.transform(g).asInstanceOf[D]

  def reflect(x: Double, y: Double): AffineTransformation = AffineTransformation(trans.reflect(x, y))

  def reflect(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation = AffineTransformation(trans.reflect(x0, y0, x1, y1))

  def rotate(theta: Double): AffineTransformation = AffineTransformation(trans.rotate(theta))

  def scale(xscale: Double, yscale: Double): AffineTransformation = AffineTransformation(trans.scale(xscale, yscale))

  def shear(xshear: Double, yshear: Double): AffineTransformation = AffineTransformation(trans.shear(xshear, yshear))

  def translate(x: Double, y: Double): AffineTransformation = AffineTransformation(trans.translate(x, y))

}
