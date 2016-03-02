package geotrellis.spark

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.vector._

/** A key for a Tile with temporal as well as spatial dimension */
case class TemporalProjectedExtent(extent: Extent, crs: CRS, instant: Long) {
  def time: DateTime = new DateTime(instant, DateTimeZone.UTC)
}

object TemporalProjectedExtent {
  def apply(extent: Extent, crs: CRS, time: DateTime): TemporalProjectedExtent =
    TemporalProjectedExtent(extent, crs, time.getMillis)

  implicit def projectedExtentComponent = new KeyComponent[TemporalProjectedExtent, ProjectedExtent] {
    def lens = createLens(
      key => ProjectedExtent(key.extent, key.crs),
      pe => key => TemporalProjectedExtent(pe.extent, pe.crs, key.instant)
    )
  }

  implicit def temporalComponent = new KeyComponent[TemporalProjectedExtent, TemporalKey] {
    def lens = createLens(
      key => TemporalKey(key.instant),
      pe => key => key
    )
  }
}
