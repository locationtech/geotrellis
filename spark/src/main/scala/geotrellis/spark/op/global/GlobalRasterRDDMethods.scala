package geotrellis.spark.op.global

import geotrellis.raster._
import geotrellis.raster.op.global._

import geotrellis.vector._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import org.apache.spark.rdd.RDD

trait GlobalRasterRDDMethods extends RasterRDDMethods {

  def convolve(kernel: Kernel): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, Convolve(r, kernel))
  }

  def costDistance(points: Seq[(Int, Int)]): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, CostDistance(r, points))
  }

  def toVector(
    extent: Extent,
    regionConnectivity: Connectivity = RegionGroupOptions.default.connectivity
  ): RDD[(Long, List[PolygonFeature[Int]])] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, ToVector(r, extent, regionConnectivity))
    }

  def regionGroup(
    options: RegionGroupOptions = RegionGroupOptions.default
  ): RDD[(Long, RegionGroupResult)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, RegionGroup(r, options))
    }

  def verticalFlip(): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, VerticalFlip(r))
  }

  def viewshed(col: Int, row: Int, exact: Boolean = false): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) =>
        if (exact)
          TmsTile(t, Viewshed(r, col, row))
        else
          TmsTile(t, ApproxViewshed(r, col, row))
    }


  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) =>
        if (exact)
          TmsTile(t, Viewshed.offsets(r, col, row))
        else
          TmsTile(t, ApproxViewshed.offsets(r, col, row))
    }
}
