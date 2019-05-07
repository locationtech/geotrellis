package geotrellis.layers.mapalgebra.focal

import geotrellis.raster.DoubleConstantNoDataCellType
import geotrellis.raster.mapalgebra.focal._

trait FocalTileLayerCollectionMethods[K] extends CollectionFocalOperation[K] {

  def focalSum(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Sum(tile, n, bounds, target) }
  def focalMin(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Min(tile, n, bounds, target) }
  def focalMax(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Max(tile, n, bounds, target) }
  def focalMean(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Mean(tile, n, bounds, target) }
  def focalMedian(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Median(tile, n, bounds, target) }
  def focalMode(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Mode(tile, n, bounds, target) }
  def focalStandardDeviation(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => StandardDeviation(tile, n, bounds, target) }
  def focalConway() = { val n = Square(1) ; focal(n) { (tile, bounds) => Sum(tile, n, bounds) } }
  def focalConvolve(k: Kernel, target: TargetCell = TargetCell.All) = { focal(k) { (tile, bounds) => Convolve(tile, k, bounds, target) } }

  /** Calculates the aspect of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Aspect]]
   */
  def aspect(target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Aspect(tile, n, bounds, cellSize, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }

  /** Calculates the slope of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Slope]]
   */
  def slope(zFactor: Double = 1.0, target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Slope(tile, n, bounds, cellSize, zFactor, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }
}
