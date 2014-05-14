package geotrellis.proj4

import org.osgeo.proj4j._

object Reproject {
  def apply(x: Double, y: Double, srcCrs: String, destCrs: String): (Double, Double) =
    apply(x, y, CRS.fromName(srcCrs), CRS.fromName(destCrs))

  def apply(x: Double, y: Double, src: CRS, dest: CRS): (Double, Double) =
    Transform(src, dest)(x, y)
}
