package geotrellis.spark.op.global

import geotrellis.raster._
import geotrellis.raster.op.global._

import geotrellis.vector._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import org.apache.spark.rdd.RDD

trait GlobalRasterRDDMethods extends RasterRDDMethods {

  def convolve(kernel: Kernel): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, Convolve(t, kernel))
  }

  def costDistance(points: Seq[(Int, Int)]): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, CostDistance(t, points))
  }

  def toVector(
    extent: Extent,
    regionConnectivity: Connectivity = RegionGroupOptions.default.connectivity
  ): RDD[(Long, List[PolygonFeature[Int]])] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, ToVector(t, extent, regionConnectivity))
    }

  def regionGroup(
    options: RegionGroupOptions = RegionGroupOptions.default
  ): RDD[(Long, RegionGroupResult)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, RegionGroup(t, options))
    }

  def verticalFlip(): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, VerticalFlip(t))
  }

  def viewshed(col: Int, row: Int, exact: Boolean = false): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) =>
        if (exact)
          TmsTile(r, Viewshed(t, col, row))
        else
          TmsTile(r, ApproxViewshed(t, col, row))
    }


  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) =>
        if (exact)
          TmsTile(r, Viewshed.offsets(t, col, row))
        else
          TmsTile(r, ApproxViewshed.offsets(t, col, row))
    }
}
