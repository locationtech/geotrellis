/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.pointcloud.spark.triangulation

import io.pdal._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.buffer.Direction
import geotrellis.pointcloud.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD
import spire.syntax.cfor._

object TinToDem {
  case class Options(
    cellType: CellType = DoubleConstantNoDataCellType,
    boundsBuffer: Option[Double] = None
  )

  object Options {
    def DEFAULT = Options()
  }

  def apply(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, extent: Extent, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] =
    rdd
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.flatMap { case (key, neighbors) =>
          val extent @ Extent(xmin, ymin, xmax, ymax) =
            layoutDefinition.mapTransform(key)

          var len = 0
          neighbors.foreach { case (_, (_, arr)) =>
            len += arr.length
          }
          val points = Array.ofDim[Coordinate](len)
          var j = 0
          options.boundsBuffer match {
            case Some(b) =>
              val bufferedExtent =
                Extent(
                  xmin - b,
                  ymin - b,
                  xmax + b,
                  ymax + b
                )

              neighbors.foreach { case (_, (_, tilePoints)) =>
                cfor(0)(_ < tilePoints.length, _ + 1) { i =>
                  val p = tilePoints(i)
                  // Only consider points within `boundsBuffer` of the extent
                  if(bufferedExtent.contains(p.x, p.y)) {
                    points(j) = p
                    j += 1
                  }
                }
              }
            case None =>
              neighbors.foreach { case (_, (_, tilePoints)) =>
                cfor(0)(_ < tilePoints.length, _ + 1) { i =>
                  points(j) = tilePoints(i)
                  j += 1
                }
              }
          }

          if(j > 2) {
            val pointSet =
              new DelaunayPointSet {
                def length: Int = j
                def getX(i: Int): Double = points(i).x
                def getY(i: Int): Double = points(i).y
                def getZ(i: Int): Double = points(i).z
              }

            val delaunay =
              PointCloudTriangulation(pointSet)

            val re =
              RasterExtent(
                extent,
                layoutDefinition.tileCols,
                layoutDefinition.tileRows
              )

            val tile =
              ArrayTile.empty(options.cellType, re.cols, re.rows)

            delaunay.rasterize(tile, re)

            Some((key, tile))
          } else {
            None
          }
        }
      }, preservesPartitioning = true)

  def withStitch(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, extent: Extent, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] = {

    // Assumes that a partitioner has already been set

    val triangulations: RDD[(SpatialKey, PointCloudTriangulation)] =
      rdd
        .mapPartitions({ partitions =>
          partitions.flatMap { case (key, points) =>
            if(points.length > 2)
              Some((key, PointCloudTriangulation(points)))
            else
              None
          }
        }, preservesPartitioning = true)

    val boundingMeshes: RDD[(SpatialKey, BoundingMesh)] =
      triangulations
        .mapPartitions({ partitions =>
          partitions.map { case (key, triangulation) =>
            val extent = layoutDefinition.mapTransform(key)
            (key, triangulation.boundingMesh(extent))
          }
        }, preservesPartitioning = true)

    boundingMeshes
      .collectNeighbors
      .join(triangulations)
      .mapPartitions({ partition =>
        partition.map { case (key, (borders: Iterable[(Direction, (SpatialKey, BoundingMesh))], triangulation: PointCloudTriangulation)) =>
        val extent = layoutDefinition.mapTransform(key)
        val re =
          RasterExtent(
            extent,
            layoutDefinition.tileCols,
            layoutDefinition.tileRows
          )

          val tile =
            ArrayTile.empty(options.cellType, re.cols, re.rows)

          triangulation.rasterize(tile, re)

          val neighbors =
            borders.map { case (d, (k, bm)) => (d, bm) }.toMap

          val stitched = triangulation.stitch(neighbors)
          stitched.rasterize(tile, re)

          (key, tile)
        }
      }, preservesPartitioning = true)
  }
}
