package geotrellis.stat

/**
  * Data object for sharing the basic statistics about a raster or region.
  */
case class Statistics(val mean:Double, val median:Int, val mode:Int,
                      val stddev:Double, val zmin:Int, val zmax:Int)
