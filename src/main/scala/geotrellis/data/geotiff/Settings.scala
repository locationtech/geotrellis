package geotrellis.data.geotiff

/**
 * Used by geotiff.Encoder to control how GeoTiff data is to be written.
 *
 * Currently supports sample size and format.
 */
case class Settings(size:SampleSize, format:SampleFormat)
