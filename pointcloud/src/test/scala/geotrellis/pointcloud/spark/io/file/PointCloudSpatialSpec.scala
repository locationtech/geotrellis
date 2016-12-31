package geotrellis.pointcloud.spark.io.file

import io.pdal._

import geotrellis.raster._
import geotrellis.pointcloud.spark._
import geotrellis.pointcloud.spark.io._
import geotrellis.pointcloud.spark.io.hadoop.HadoopPointCloudRDD
import geotrellis.proj4.CRS
import geotrellis.spark.{ContextRDD, KeyBounds, LayerId, SpatialKey, TileLayerMetadata}
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import geotrellis.spark.io.index._
import geotrellis.spark.tiling._
import geotrellis.vector.Extent
import org.scalatest._

class PointCloudSpatialSpec extends FunSpec
  with Matchers
  with PointCloudTestEnvironment {

  describe("PointCloud File IO") {
    val layerId = LayerId("pc", 0)
    val extent = Extent(635609.85, 848889.7, 638992.55, 853545.43)
    val crs = CRS.fromEpsgCode(20255)
    val rdd = HadoopPointCloudRDD(lasPath).flatMap(_._2)
    val layoutDefinition = LayoutDefinition(
      extent,
      TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10))
    val tiledWithLayout = rdd.tileToLayout(layoutDefinition)
    val gb = layoutDefinition.mapTransform(extent)

    val md =
      TileLayerMetadata[SpatialKey](
        cellType = DoubleConstantNoDataCellType,
        layout = layoutDefinition,
        extent = extent,
        crs = crs,
        bounds = KeyBounds(gb)
      )

    val tiled = ContextRDD(tiledWithLayout, md)

    lazy val reader = FileLayerReader(outputLocalPath)
    lazy val writer = FileLayerWriter(outputLocalPath)

    it("should not find layer before write") {
      intercept[LayerNotFoundError] {
        reader.read[SpatialKey, PointCloud, TileLayerMetadata[SpatialKey]](layerId)
      }
    }

    it("should write layer") {
      writer.write[SpatialKey, PointCloud, TileLayerMetadata[SpatialKey]](layerId, tiled, ZCurveKeyIndexMethod)
    }

    it("should read layer") {
      val actual = reader.read[SpatialKey, PointCloud, TileLayerMetadata[SpatialKey]](layerId).map(_._1).collect()
      val expected = tiled.map(_._1).collect()

      if (expected.diff(actual).nonEmpty)
        info(s"missing: ${(expected diff actual).toList}")
      if (actual.diff(expected).nonEmpty)
        info(s"unwanted: ${(actual diff expected).toList}")

      actual should contain theSameElementsAs expected
    }
  }

}
