package geotrellis.data.geotiff

private[geotiff] 
case class Nodata(value: Double, isSet: Boolean) {
  /**
   * Int nodata value to use when writing raster.
   */
  def toInt(settings: Settings): Int = (settings.format, settings.nodata) match {
    case (Signed | Unsigned, Nodata(d, true)) => d.toInt
    case (Signed, Nodata(_, false))           => (1L << (settings.size.bits - 1)).toInt
    case (Unsigned, Nodata(_, false))         => ((1L << settings.size.bits) - 1).toInt
    case (Floating, _)                        => sys.error("floating point not supported")
  }

  /**
   * String nodata value to use in GeoTIFF metadata.
   */
  def toString(settings: Settings): String = (settings.format, settings.nodata) match {
	/* first two cases represent when nodata value was set (NoData.isSet = true) */  
    case (Signed | Unsigned, Nodata(d, true)) => d.toLong.toString
    case (Floating, Nodata(d, true)) => if (settings.esriCompat)
      Double.MinValue.toString
    else
      d.toFloat.toString
  	/* next cases represent when nodata value was not set (NoData.isSet = false) */  
    case (Signed, Nodata(_, false))   => "-" + (1L << (settings.size.bits - 1)).toString
    case (Unsigned, Nodata(_, false)) => ((1L << settings.size.bits) - 1).toString
    case (Floating, Nodata(_, false)) => if (settings.esriCompat)
      Double.MinValue.toString
    else
      Double.NaN.toString
  }
}

object Nodata {
  val Default = Nodata(Double.NaN, false)
}