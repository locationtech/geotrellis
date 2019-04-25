package geotrellis

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.util._

import org.locationtech.proj4j.UnsupportedParameterException

package object tiling extends Implicits {
  type TileBounds = GridBounds[Int]

  type SpatialComponent[K] = Component[K, SpatialKey]
  type TemporalComponent[K] = Component[K, TemporalKey]

  private final val WORLD_WSG84 = Extent(-180, -89.99999, 179.99999, 89.99999)

  implicit class CRSWorldExtent(val crs: CRS) extends AnyVal {
    def worldExtent: Extent =
      crs match {
        case LatLng =>
          WORLD_WSG84
        case WebMercator =>
          Extent(-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244)
        case Sinusoidal =>
          Extent(-2.0015109355797417E7, -1.0007554677898709E7, 2.0015109355797417E7, 1.0007554677898709E7)
        case c if c.proj4jCrs.getProjection.getName == "utm" =>
          throw new UnsupportedParameterException(
            s"Projection ${c.toProj4String} is not supported as a WorldExtent projection, " +
            s"use a different projection for your purposes or use a different LayoutScheme."
          )
        case _ =>
          WORLD_WSG84.reproject(LatLng, crs)
      }
  }
}
