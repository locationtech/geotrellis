package geotrellis.statistics

/**
  * Data object for sharing the basic statistics about a raster or region.
  */
case class Statistics(mean:Double = Double.NaN, median:Int = geotrellis.NODATA,
                      mode:Int = geotrellis.NODATA, stddev:Double = Double.NaN,
                      zmin:Int = geotrellis.NODATA, zmax:Int = geotrellis.NODATA)

object Statistics {
  val EMPTY = Statistics()
}
