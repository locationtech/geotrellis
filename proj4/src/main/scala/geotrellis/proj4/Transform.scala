package geotrellis.proj4

import org.osgeo.proj4j._

object Transform {
  def apply(src: CRS, dest: CRS): (Double, Double) => (Double, Double) =
    Backend.getTransform(src, dest)
}
