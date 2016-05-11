package geotrellis.spark

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.util._

import com.github.nscala_time.time.Imports._

/** A key for a Tile with temporal as well as spatial dimension */
case class TemporalProjectedExtent(extent: Extent, crs: CRS, instant: Long) {
  def time: DateTime = new DateTime(instant, DateTimeZone.UTC)
  def projectedExtent = ProjectedExtent(extent, crs)
}

object TemporalProjectedExtent {
  def apply(extent: Extent, crs: CRS, time: DateTime): TemporalProjectedExtent =
    TemporalProjectedExtent(extent, crs, time.getMillis)

  def apply(projectedExtent: ProjectedExtent, time: DateTime): TemporalProjectedExtent =
    TemporalProjectedExtent(projectedExtent.extent, projectedExtent.crs, time.getMillis)

  def apply(projectedExtent: ProjectedExtent, instant: Long): TemporalProjectedExtent =
    TemporalProjectedExtent(projectedExtent.extent, projectedExtent.crs, instant)

  implicit val projectedExtentComponent =
    Component[TemporalProjectedExtent, ProjectedExtent](
      k => k.projectedExtent,
      (k, pe) => TemporalProjectedExtent(pe, k.instant)
    )

  implicit val temporalComponent =
    Component[TemporalProjectedExtent, TemporalKey](
      k => TemporalKey(k.instant),
      (k, tk) => TemporalProjectedExtent(k.extent, k.crs, tk.instant)
    )
}
