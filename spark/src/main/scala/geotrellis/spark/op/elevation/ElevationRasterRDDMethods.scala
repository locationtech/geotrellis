package geotrellis.spark.op.elevation

import geotrellis.raster._
import geotrellis.raster.op.elevation._
import geotrellis.raster.op.focal._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait ElevationRasterRDDMethods extends RasterRDDMethods {
  /**
   * Calculates the slope of each cell in a raster.
   * @param   cs         cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   * @see [[Slope]]
   */
  def slope(
    cs: CellSize,
    zFactor: Double = 1.0,
    bounds: Option[GridBounds] = None): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Slope(r, Square(1), bounds, cs, zFactor))
  }

  /**
   * Calculates the aspect of each cell in a raster.
   * @param   cs          cellSize of the raster
   * @see [[Aspect]]
   */
  def aspect(cs: CellSize, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Aspect(r, Square(1), bounds, cs))
    }


  /**
   * Computes Hillshade (shaded relief) from a raster.
   * @see [[Hillshade]]
   */
  def hillshade(
    cs: CellSize,
    azimuth: Double = 315,
    altitude: Double = 45,
    zFactor: Double = 1.0,
    bounds: Option[GridBounds] = None): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) =>
      TmsTile(t, Hillshade(r, Square(1), bounds, cs, azimuth, altitude, zFactor))
  }
}
