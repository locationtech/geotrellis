package geotrellis.raster.interpolation

import geotrellis.vector.interpolation.SimpleKriging

/** Methods implicitly added to tile via the package import.
  * Should contain a method for each overloaded way to create a SimpleKriging
  */
trait SimpleKrigingMethods {
  val tile: Tile

  def simpleKriging(extent: Extent, points: Array[PointFeature[Double]], bandwidth: Double) = 
    Interpolation(tile, extent)(SimpleKriging(points, bandwidth))
}
