package geotrellis.data.geotiff

/**
 * Used by geotiff.Encoder to control how GeoTiff data is to be written.
 *
 * Currently supports sample size, format and 'esriCompat', an ESRI
 * compatibility option.
 *
 * This compatibility option changes the way that geographic data are written.
 * The "normal" approach is similar to how GDAL and other strict GeoTIFF
 * encoders work (we write out the projected CS ID, i.e. 3857). If 'esriCompat'
 * is set to true, we instead write out a "user-defined" projected CS.
 */
case class Settings(size:SampleSize, format:SampleFormat, esriCompat:Boolean)
