package geotrellis.raster

// --- //

/** Values chosen to represent raster cell ''NODATA'' for various numeric types. */
object NoData {
  val byte: Byte = Byte.MinValue

  val short: Short = Short.MinValue

  val int: Int = Int.MinValue

  val float: Float = Float.NaN

  val double: Double = Double.NaN
}
