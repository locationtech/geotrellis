package geotrellis.proj4

import org.osgeo.proj4j._

object Transform {
  def apply(src: CRS, dest: CRS): (Double, Double) => (Double, Double) =
    src.alternateTransform(dest) match {
      case Some(f) => f
      case None => Proj4Transform(src, dest)
    }
}

object Proj4Transform {
  def apply(src: CRS, dest: CRS): Transform = {
    val t = new BasicCoordinateTransform(src.crs, dest.crs)

    { (x: Double, y: Double) =>
      val srcP = new ProjCoordinate(x, y)
      val destP = new ProjCoordinate
      t.transform(srcP, destP)
      (destP.x, destP.y)
    }
  }
}
