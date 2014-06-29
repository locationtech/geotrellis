package geotrellis.proj4

import org.osgeo.proj4j._

object Transform {
  private val transformFactory = new CoordinateTransformFactory

  def apply(src: CRS, dest: CRS): Transform =
    new Transform(src, dest)
}

class Transform(src: CRS, dest: CRS) {
  val t = Transform.transformFactory.createTransform(src.crs, dest.crs)

  def apply(x: Double, y: Double): (Double, Double) = {
    val srcP = new ProjCoordinate(x, y)
    val destP = new ProjCoordinate
    t.transform(srcP, destP)
    (destP.x, destP.y)
  }
}
