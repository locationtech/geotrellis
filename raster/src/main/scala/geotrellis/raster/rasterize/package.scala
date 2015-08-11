package geotrellis.raster

package object rasterize {

  /** Callback for given row and column (compatible with previous definition). */
  type Callback = (Int, Int) => Unit

}
