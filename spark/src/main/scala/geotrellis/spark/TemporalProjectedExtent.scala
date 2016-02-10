package geotrellis.spark

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.vector._

/** A key for a Tile with temporal as well as spatial dimension */
case class TemporalProjectedExtent(extent: Extent, crs: CRS, time: DateTime)

object TemporalProjectedExtent {
  implicit def ingestKey = new KeyComponent[TemporalProjectedExtent, ProjectedExtent] {
    def lens = createLens(
      key => ProjectedExtent(key.extent, key.crs),
      pe => key => TemporalProjectedExtent(pe.extent, pe.crs, key.time)
    )
  }

  implicit def ingestTemporalKey = new KeyComponent[TemporalProjectedExtent, TemporalKey] {
    def lens = createLens(
      key => TemporalKey(key.time),
      pe => key => key
    )
  }
}
