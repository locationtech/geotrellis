package geotrellis.proj4

import geotrellis.proj4.CRS
import geotrellis.proj4.CRS.ObjectNameToString

object Ellipsoidal extends CRS with ObjectNameToString {
  lazy val proj4jCrs = factory.createFromName("EPSG:3785")

  override val epsgCode: Option[Int] = Some(3785)
}
