package geotrellis.spark.pointcloud.triangulation

import io.pdal._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.apache.spark.rdd.RDD
import spire.syntax.cfor._

import scala.collection.mutable

object TinToDem {
  def apply(rdd: RDD[PointCloud], layoutDefinition: LayoutDefinition, extent: Extent, numPartitions: Int): RDD[(SpatialKey, Tile)] =
    rdd
      .flatMap { case pointCloud =>
        var lastKey: SpatialKey = null
        val keysToPoints = mutable.Map[SpatialKey, mutable.ArrayBuffer[LightPoint]]()
        val pointSize = pointCloud.pointSize

        cfor(0)(_ < pointSize, _ + 1) { i =>
          val x = pointCloud.getX(i)
          val y = pointCloud.getY(i)
          val z = pointCloud.getZ(i)
          val p = LightPoint(x, y, z)
          val key = layoutDefinition.mapTransform(x, y)
          if(key == lastKey) {
            keysToPoints(lastKey) += p
          } else if(keysToPoints.contains(key)) {
            keysToPoints(key) += p
            lastKey = key
          } else {
            keysToPoints(key) = mutable.ArrayBuffer(p)
            lastKey = key
          }
        }

        keysToPoints.map { case (k, v) => (k, v.toArray) }
      }
      .reduceByKey({ (p1, p2) => p1 union p2 }, numPartitions)
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.map { case (key, neighbors) =>
          val extent = layoutDefinition.mapTransform(key)


          var len = 0
          neighbors.foreach { case (_, (_, arr)) =>
            len += arr.length
          }
          val points = Array.ofDim[LightPoint](len)
          var j = 0
          neighbors.foreach { case (_, (_, tilePoints)) =>
            cfor(0)(_ < tilePoints.length, _ + 1) { i =>
              points(j) = tilePoints(i)
              j += 1
            }
          }

          val delaunay =
            PointCloudTriangulation(points)

          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          val tile =
            ArrayTile.empty(DoubleConstantNoDataCellType, re.cols, re.rows)

          delaunay.rasterize(tile, re)

          (key, tile)
        }
      }, preservesPartitioning = true)
}
