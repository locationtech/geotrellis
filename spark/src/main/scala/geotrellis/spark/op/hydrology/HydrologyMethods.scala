package geotrellis.spark.op.hydrology

import geotrellis.raster.op.focal.Square
import geotrellis.raster.op.hydrology._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait HydrologyRasterRDDMethods extends RasterRDDMethods {

  def accumulation(): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Accumulation(r))
  }

  /**  Operation to compute a flow direction raster from an elevation raster
    * @see [[FlowDirection]]
    */
  def flowDirection(): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, FlowDirection(r))
  }

  /** Fills sink values in a raster. Returns a RasterRDD of TypeDouble
    * @see [[Fill]]
    */
  def fill(threshold: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Fill(r, Square(1), None, threshold))
  }
}
