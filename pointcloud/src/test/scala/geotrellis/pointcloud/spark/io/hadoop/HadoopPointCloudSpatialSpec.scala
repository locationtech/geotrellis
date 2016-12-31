package geotrellis.pointcloud.spark.io.hadoop

import io.pdal._

import geotrellis.raster._
import geotrellis.pointcloud.spark._
import geotrellis.pointcloud.spark.io._
import geotrellis.spark.{LayerId, SpatialKey, TileLayerMetadata}
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles

import org.scalatest._

class HadoopPointCloudSpatialSpec
  extends PersistenceSpec[SpatialKey, PointCloud, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with PointCloudTestEnvironment
    with TestFiles
    with PointCloudSpatialTestFiles {

  lazy val reader = HadoopLayerReader(outputLocal)
  lazy val creader = HadoopCollectionLayerReader(outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier(outputLocal)
  lazy val mover  = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val updater = HadoopLayerUpdater(outputLocal)
  lazy val tiles = HadoopValueReader(outputLocal)
  lazy val sample = pointCloudSample

  describe("HDFS layer names") {
    it("should handle layer names with spaces") {
      val layer = sample
      val layerId = LayerId("Some layer", 10)

      writer.write[SpatialKey, PointCloud, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
    }

    it("should fail gracefully with colon in name") {
      val layer = sample
      val layerId = LayerId("Some:layer", 10)

      intercept[InvalidLayerIdError] {
        writer.write[SpatialKey, PointCloud, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      }
    }
  }
}
