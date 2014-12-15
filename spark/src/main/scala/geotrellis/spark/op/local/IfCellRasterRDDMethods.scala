package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.IfCell

trait IfCellRasterRDDMethods[K] extends RasterRDDMethods[K] {

  def localIf(cond: Int => Boolean, trueValue: Int): RasterRDD[K] = 
    rasterRDD
      .mapRows { case (t, r) =>
        (t, IfCell(r, cond, trueValue))
      }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double): RasterRDD[K] = rasterRDD.mapRows {
    case (t, r) => (t, IfCell(r, cond, trueValue))
  }

  def localIf(
    cond: Int => Boolean,
    trueValue: Int,
    falseValue: Int): RasterRDD[K] = rasterRDD.mapRows {
    case (t, r) => (t, IfCell(r, cond, trueValue, falseValue))
  }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double,
    falseValue: Double): RasterRDD[K] = rasterRDD.mapRows {
    case (t, r) => (t, IfCell(r, cond, trueValue, falseValue))
  }

  def localIf(
    other: RasterRDD[K],
    cond: (Int, Int) => Boolean,
    trueValue: Int): RasterRDD[K] = rasterRDD.combineRows(other) {
    case ((t1, r1), (t2, r2)) =>
      (t1, IfCell(r1, r2, cond, trueValue))
  }

  def localIf(
    other: RasterRDD[K],
    cond: (Double, Double) => Boolean,
    trueValue: Double): RasterRDD[K] = rasterRDD.combineRows(other) {
    case ((t1, r1), (t2, r2)) =>
      (t1, IfCell(r1, r2, cond, trueValue))
  }

  def localIf(
    other: RasterRDD[K],
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int): RasterRDD[K] = rasterRDD.combineRows(other) {
    case ((t1, r1), (t2, r2)) =>
      (t1, IfCell(r1, r2, cond, trueValue, falseValue))
  }

  def localIf(
    other: RasterRDD[K],
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double): RasterRDD[K] = rasterRDD.combineRows(other) {
    case ((t1, r1), (t2, r2)) =>
      (t1, IfCell(r1, r2, cond, trueValue, falseValue))
  }

}
