package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.IfCell
import geotrellis.spark.rdd.RasterRDD

trait IfCellRasterRDDMethods extends RasterRDDMethods {

  def localIf(
    cond: Int => Boolean,
    trueValue: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, IfCell(r, cond, trueValue))
  }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, IfCell(r, cond, trueValue))
  }

  def localIf(
    cond: Int => Boolean,
    trueValue: Int,
    falseValue: Int): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, IfCell(r, cond, trueValue, falseValue))
  }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double,
    falseValue: Double): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, IfCell(r, cond, trueValue, falseValue))
  }

  def localIf(
    other: RasterRDD,
    cond: (Int, Int) => Boolean,
    trueValue: Int): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) =>
      TmsTile(t1, IfCell(r1, r2, cond, trueValue))
  }

  def localIf(
    other: RasterRDD,
    cond: (Double, Double) => Boolean,
    trueValue: Double): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) =>
      TmsTile(t1, IfCell(r1, r2, cond, trueValue))
  }

  def localIf(
    other: RasterRDD,
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) =>
      TmsTile(t1, IfCell(r1, r2, cond, trueValue, falseValue))
  }

  def localIf(
    other: RasterRDD,
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double): RasterRDD = rasterRDD.combineTiles(other) {
    case (TmsTile(t1, r1), TmsTile(t2, r2)) =>
      TmsTile(t1, IfCell(r1, r2, cond, trueValue, falseValue))
  }

}
