
package geotrellis.raster.interpolation

/** Methods implicitly added to tile via the package import.
  * Should contain a method for each overloaded way to create a SimpleKriging
  */
trait OrdinaryKrigingMethods extends KrigingMethods {
  val tile: Tile

  def ordinaryKriging(extent: Extent, points: Array[PointFeature[Double]], bandwidth: Double) = {
    val kriging =  SimpleKriging(points, bandwidth)
    Interpolation(tile, extent) { (x: Double, y: Double) => kriging(x, y)._1 }
  }
}
