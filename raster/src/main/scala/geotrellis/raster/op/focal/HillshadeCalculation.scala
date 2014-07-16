package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.raster.op.focal.Angles._

import scala.math._

/**
 * Computes Hillshade (shaded relief) from a raster.
 *
 * The resulting raster will be a shaded relief map (a hill shading)
 * based on the sun altitude, azimuth, and the z factor. The z factor is
 * a conversion factor from map units to elevation units.
 *
 * Returns a raster of TypeShort.
 *
 * This operation uses Horn's method for computing hill shading.
 *
 * @see [[http://goo.gl/DtVDQ Esri Desktop's description of Hillshade.]]
 */
object HillshadeCalculation {

  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] with Initialization4[CellSize, Double, Double, Double] = {
    new SurfacePointCalculation[Tile] with ShortArrayTileResult
      with Initialization4[CellSize, Double, Double, Double] {
      var azimuth = 0.0
      var zenith = 0.0
      var zFactor = 0.0

      // Caches trig values for speed
      var cosZ = 0.0
      var sinZ = 0.0
      var cosAz = 0.0
      var sinAz = 0.0

      def init(r: Tile, cs: CellSize, az: Double, al: Double, z: Double) = {
        super.init(r)

        cellSize = cs
        azimuth = radians(90.0 - az)
        zenith = radians(90.0 - al)
        zFactor = z

        cosZ = cos(zenith)
        sinZ = sin(zenith)
        cosAz = cos(azimuth)
        sinAz = sin(azimuth)
      }

      def setValue(x: Int, y: Int, s: SurfacePoint) {
        val slope = s.slope(zFactor)
        val aspect = s.aspect

        val c = cosAz * s.cosAspect + sinAz * s.sinAspect // cos(azimuth - aspect)
        val v = (cosZ * s.cosSlope) + (sinZ * s.sinSlope * c)
        tile.set(x, y, round(127.0 * max(0.0, v)).toInt)
      }
    }
  }

  /** Indirect calculation of hill shading of a tile that uses Aspect and Slope operation results */
  def indirect(aspect: Tile, slope: Tile, azimuth: Double, altitude: Double): Tile = {
    val az = radians(90.0 - azimuth)
    val ze = radians(90.0 - altitude)
    val cosZe = cos(ze)
    val sinZe = sin(ze)

    val hr = aspect.combineDouble(slope) { (aspectValue, slopeValue) =>
      val slopeRads = radians(slopeValue)
      val aspectRads = radians(aspectValue)
      val v = (cosZe * cos(slopeRads)) +
        (sinZe * sin(slopeRads) * cos(az - aspectRads))
      round(127.0 * max(0.0, v))
    }
    hr.convert(TypeShort)
  }
}
