package geotrellis.spark

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.util._

import java.time.{ZoneOffset, ZonedDateTime}

/** A key for a Tile with temporal as well as spatial dimension */
case class TemporalProjectedExtent(extent: Extent, crs: CRS, instant: Long) {
  def time: ZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
  def projectedExtent = ProjectedExtent(extent, crs)
}

object TemporalProjectedExtent {
  def apply(extent: Extent, crs: CRS, time: ZonedDateTime): TemporalProjectedExtent =
    TemporalProjectedExtent(extent, crs, time.toInstant.toEpochMilli)

  def apply(projectedExtent: ProjectedExtent, time: ZonedDateTime): TemporalProjectedExtent =
    TemporalProjectedExtent(projectedExtent.extent, projectedExtent.crs, time.toInstant.toEpochMilli)

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
