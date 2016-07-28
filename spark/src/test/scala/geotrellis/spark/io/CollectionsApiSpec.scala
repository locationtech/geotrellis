package geotrellis.spark.io

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.io.file.FileLayerCollectionReader
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.util._
import geotrellis.vector._
import org.joda.time.DateTime

trait CollectionsApiSpec { self: PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] with TestEnvironment =>

  for(PersistenceSpecDefinition(keyIndexMethodName, keyIndexMethod, layerIds) <- specLayerIds) {
    val layerId = layerIds.layerId

    //creader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)

    describe(s"collections api tests") {
      it("should not find layer before write (collections api)") {
        intercept[LayerNotFoundError] {
          creader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
        }
      }

      it("should read a layer back (collections api)") {
        val actual = creader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId).map(_._1)
        val expected = sample.keys.collect()

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }
    }
  }
}
