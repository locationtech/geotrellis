package geotrellis.spark.io

trait Driver[K] {
  /** Driver specific parameter required to save a raster */
  type Params
}
