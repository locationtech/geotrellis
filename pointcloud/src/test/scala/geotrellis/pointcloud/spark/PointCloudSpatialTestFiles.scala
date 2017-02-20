package geotrellis.pointcloud.spark

import com.vividsolutions.jts.geom.Coordinate
import geotrellis.pointcloud.spark._
import geotrellis.pointcloud.spark.io._
import geotrellis.pointcloud.spark.io.hadoop._
import geotrellis.proj4.CRS
import geotrellis.raster.{DoubleConstantNoDataCellType, TileLayout}
import geotrellis.spark.{ContextRDD, KeyBounds, SpatialKey, TileLayerMetadata}
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.util.KryoWrapper
import geotrellis.vector.Extent
import spire.syntax.cfor.cfor

import scala.collection.mutable

trait PointCloudSpatialTestFiles extends Serializable { self: PointCloudTestEnvironment =>
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

  val pointCloudSample = ContextRDD(tiledWithLayout, md)

  val rddc = HadoopPointCloudRDD(lasPath).flatMap { case (_, pointClouds) =>
    val extent = Extent(635609.85, 848889.7, 638992.55, 853545.43)
    val layoutDefinition = LayoutDefinition(
      extent,
      TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10))
    val mapTransform = layoutDefinition.mapTransform

    var lastKey: SpatialKey = null
    val keysToPoints = mutable.Map[SpatialKey, mutable.ArrayBuffer[Coordinate]]()

    for (pointCloud <- pointClouds) {
      val len = pointCloud.length
      cfor(0)(_ < len, _ + 1) { i =>
        val x = pointCloud.getX(i)
        val y = pointCloud.getY(i)
        val z = pointCloud.getZ(i)
        val p = new Coordinate(x, y, z)
        val key = mapTransform(x, y)
        if (key == lastKey) {
          keysToPoints(lastKey) += p
        } else if (keysToPoints.contains(key)) {
          keysToPoints(key) += p
          lastKey = key
        } else {
          keysToPoints(key) = mutable.ArrayBuffer(p)
          lastKey = key
        }
      }
    }

    keysToPoints.map { case (k, v) => (k, v.toArray) }
  }
  .reduceByKey(_ ++ _).filter { _._2.length > 2 }

  val pointCloudSampleC = ContextRDD(rddc, md)

}
