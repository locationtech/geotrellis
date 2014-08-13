package geotrellis.proj4

import org.osgeo.proj4j._

object Reproject {
  def apply(x: Double, y: Double, src: String, dest: String): (Double, Double) =
    apply(x, y, CRS.fromName(src), CRS.fromName(dest))

  def apply(x: Double, y: Double, src: CRS, dest: CRS): (Double, Double) = {
    Transform(src, dest)(x, y)
  }
}
