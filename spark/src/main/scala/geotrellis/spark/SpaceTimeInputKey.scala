package geotrellis.spark

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.vector._

/** A key for a Tile with temporal as well as spatial dimension */
case class SpaceTimeInputKey(extent: Extent, crs: CRS, time: DateTime)

object SpaceTimeInputKey {
  implicit def ingestKey = new KeyComponent[SpaceTimeInputKey, ProjectedExtent] {
    def lens = createLens(
      key => ProjectedExtent(key.extent, key.crs),
      pe => key => SpaceTimeInputKey(pe.extent, pe.crs, key.time)
    )
  }

  implicit def ingestTemporalKey = new KeyComponent[SpaceTimeInputKey, TemporalKey] {
    def lens = createLens(
      key => TemporalKey(key.time),
      pe => key => key
    )
  }
}
