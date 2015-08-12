package geotrellis.raster

package object interpolation {
  // TODO: Add Geo and Universal
  implicit class InterpolationExtensions(val tile: Tile) 
      extends SimpleKrigingMethods 
      with OrdinaryKrigingMethods
}



//
//  val tile: Tile = ???
//  tile.simpleKriging(points, bandwidth) // sensible defaults
//
