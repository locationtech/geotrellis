package geotrellis.raster.io.geotiff

sealed trait OverviewStrategy
/**
  * Specify Auto-n where n is an integer greater or equal to 0,
  * to select an overview level below the AUTO one (of a higher or equal resolution).
  */
case class Auto(n: Int = 0) extends OverviewStrategy {
  require(n >= 0, s"n should be positive as it's index in the list of overviews, given n is: $n")
}
/**
  * Selects the best matching overview where overview resolution would be higher or equal to desired
  * to prevent data loss, it is the Default strategy.
  * Chooses the base layer if there would be no good enough overview.
  */
case object AutoHigherResolution extends OverviewStrategy
/**
  * Force the base resolution to be used.
  */
case object Base extends OverviewStrategy
