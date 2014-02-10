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
case class Settings(size:SampleSize, format:SampleFormat,
                    esriCompat:Boolean, compression:Compression, nodata: Nodata = Nodata.Default) {
  def setSize(s:SampleSize) = Settings(s, format, esriCompat, compression)
  def setFormat(s:SampleFormat) = Settings(size, s, esriCompat, compression)
  def setEsriCompat(e:Boolean) = Settings(size, format, e, compression)
  def setCompression(c:Compression) = Settings(size, format, esriCompat, c)
  def setNodata(d:Double) = Settings(size, format, esriCompat, compression, Nodata(d, true))
  
  def nodataInt = nodata.toInt(this)
  def nodataString = nodata.toString(this)
}

object Settings {
  def uint8 = Settings(ByteSample, Unsigned, false, Uncompressed)
  def uint16 = Settings(ShortSample, Unsigned, false, Uncompressed)
  def uint32 = Settings(IntSample, Unsigned, false, Uncompressed)

  def int8 = Settings(ByteSample, Signed, false, Uncompressed)
  def int16 = Settings(ShortSample, Signed, false, Uncompressed)
  def int32 = Settings(IntSample, Signed, false, Uncompressed)

  def float32 = Settings(IntSample, Floating, false, Uncompressed)
  def float64 = Settings(LongSample, Floating, false, Uncompressed)
}
