package geotrellis.spark.io

import geotrellis.raster._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import geotrellis.spark.io.index._
import geotrellis.vector.Extent

import org.scalatest.FunSpec


class TileFeatureIOSpec
    extends FunSpec
    with TestEnvironment {

  describe("TileFeature AvroRecordCodec") {
    val path = "raster-test/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val outRdd = rdd.withContext {
      _.map({ case (key,tile) => key -> TileFeature(tile,tile) })
    }

    val toDisk = outRdd
      .collect
      .sortWith((a,b) => a.toString.compareTo(b.toString) < 0)

    val writer = FileLayerWriter(outputLocalPath)
    val reader = FileLayerReader(outputLocalPath)
    val deleter = FileLayerDeleter(outputLocalPath)

    it("should allow proper writing and reading") {
      val layerId = LayerId("TileFeatureLayer", 0)

      try {
        writer.write[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]](layerId, outRdd, ZCurveKeyIndexMethod)

        val fromDisk = reader.read[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]](layerId)
          .collect
          .sortWith((a,b) => a.toString.compareTo(b.toString) < 0)

        toDisk.zip(fromDisk).map({ case (a,b) =>
          a should equal (b)
        })
      }
      finally {
        deleter.delete(layerId)
      }
    }

  }
}
