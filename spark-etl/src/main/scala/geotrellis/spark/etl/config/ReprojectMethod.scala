package geotrellis.spark.etl.config

sealed trait ReprojectMethod

/**
  * BufferedReproject method will perform reproject still after the tiling step.
  * Because tiling step creates keys with SpatialComponent this method of reprojection is able to sample pixels past the
  * tile boundaries by performing a spatial neighborhood join. This method is the default and produces the best results.
  * Note that method of reprojection requires that all of the source tiles share the same CRS.
  */
case object BufferedReproject extends ReprojectMethod

/**
  * PerTileReproject method will perform reproject step before the tiling step.
  * This method of reprojection can not consider pixels past the individual tile boundaries,
  * even if they exist elsewhere in the dataset, and will read them as NODATA when interpolating.
  * However this restriction allows for source tiles to have projections that differ per tile.
  * The projections will be unified before the tiling step, which requires all extents to be in the same projection.
  */
case object PerTileReproject extends ReprojectMethod

object ReprojectMethod {
  def fromString(s: String) = s match {
    case "buffered" => BufferedReproject
    case "per-tile" => PerTileReproject
    case _ => throw new Exception(s"unsupported repreoject method: $s")
  }
}
