package geotrellis.proj4

import org.osgeo.proj4j._

object Transform {
  private[proj4] val transformFactory = new CoordinateTransformFactory

  def apply(src: CRS, dest: CRS): (Double, Double) => (Double, Double) =
    src.alternateTransform(dest) match {
      case Some(f) => f
      case None => new Proj4Transform(src, dest)
    }
}

class Proj4Transform(src: CRS, dest: CRS) extends Function2[Double, Double, (Double, Double)] {
  val t = Transform.transformFactory.createTransform(src.crs, dest.crs)

  def apply(x: Double, y: Double): (Double, Double) = {
    val srcP = new ProjCoordinate(x, y)
    val destP = new ProjCoordinate
    t.transform(srcP, destP)
    (destP.x, destP.y)
  }
}
