package geotrellis.spark.ingest

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.vector._

/** A key for a Tile with temporal as well as spatial dimension */
case class SpaceTimeInputKey(extent: Extent, crs: CRS, time: DateTime)

object SpaceTimeInputKey {
  // Allows us to view and modify only the spatial component of this key
  implicit def ingestKey = new KeyComponent[SpaceTimeInputKey, ProjectedExtent] {
    def lens = createLens(
      key => ProjectedExtent(key.extent, key.crs),
      pe => key => SpaceTimeInputKey(pe.extent, pe.crs, key.time)
    )
  }

  implicit class withSpaceTimeInputTilerKeyMethods(val self: SpaceTimeInputKey) extends TilerKeyMethods[SpaceTimeInputKey, SpaceTimeKey] {
    def extent = self.extent
    def translate(spatialKey: SpatialKey): SpaceTimeKey = SpaceTimeKey(spatialKey, self.time)
  }

}
