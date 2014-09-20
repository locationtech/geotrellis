package geotrellis.spark.op.hydrology

import geotrellis.raster.op.focal.Square
import geotrellis.raster.op.hydrology._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait HydrologyRasterRDDMethods extends RasterRDDMethods {

  def accumulation() = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, Accumulation(t))
  }

  /**  Operation to compute a flow direction raster from an elevation raster
    * @see [[FlowDirection]]
    */
  def flowDirection() = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, FlowDirection(t))
  }

  /** Fills sink values in a raster. Returns a RasterRDD of TypeDouble
    * @see [[Fill]]
    */
  def fill(threshold: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, Fill(t, Square(1), None, threshold))
  }
}
