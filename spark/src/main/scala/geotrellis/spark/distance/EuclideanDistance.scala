/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.distance

import org.locationtech.jts.geom.Coordinate
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.raster.distance._
import geotrellis.spark._
import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.voronoi._

import scala.collection.mutable.{ListBuffer, Set}

object EuclideanDistance {

  private[spark] def voronoiCells(centerStitched: StitchedDelaunay, initialEdge: Int, extent: Extent): Seq[(Polygon, Coordinate)] = {
    import centerStitched.halfEdgeTable._

    val queue = ListBuffer[(Int, Int)]((initialEdge, getDest(initialEdge)))
    val visited = Set.empty[Int]
    val result = ListBuffer.empty[(Polygon, Coordinate)]

    while (queue nonEmpty) {
      val (incoming, here) = queue.remove(0)

      if (!visited.contains(here)) {
        visited += here

        val poly = VoronoiDiagram.polygonalCell(centerStitched.halfEdgeTable, centerStitched.indexToCoord, extent)(incoming)

        if (poly isDefined) {
          result += ((poly.get, centerStitched.indexToCoord(here)))
          var e = getFlip(incoming)
          do {
            queue += ((e, getDest(e)))
            e = rotCWSrc(e)
          } while (e != getFlip(incoming))
        }
      }
    }

    result
  }

  private[spark] def neighborEuclideanDistance(center: DelaunayTriangulation, neighbors: Map[Direction, (BoundaryDelaunay, Extent)], re: RasterExtent): Option[Tile] = {
    val _neighbors = neighbors.map { case (dir, value) => (convertDirection(dir), value) }
    val stitched = StitchedDelaunay(center, _neighbors, false)

    def findBaseEdge(): Int = {
      import stitched.halfEdgeTable._

      var e = 0
      var bestdist = 1.0/0.0
      var best = -1
      do {
        while (getDest(e) == -1 && e < maxEdgeIndex)
          e += 1
        val dist = re.extent.distance(Point.jtsCoord2Point(stitched.indexToCoord(getDest(e))))
        if (dist < bestdist) {
          best = e
          bestdist = dist
        }
        e += 1
      } while (bestdist > 0 && e < maxEdgeIndex)

      best
    }

    if (stitched.pointSet.length == 0) {
      None
    } else {
      val baseEdge = 
        if (center.boundary != -1) {
          // center had edges
          stitched.halfEdgeTable.edgeIncidentTo(center.halfEdgeTable.getDest(center.boundary)) 
        } else {
          // center either has 1 or no points
          findBaseEdge()
        }

      val extent = re.extent
      val cells = voronoiCells(stitched, baseEdge, extent)

      val tile = DoubleArrayTile.empty(re.cols, re.rows)
      cells.foreach(EuclideanDistanceTile.rasterizeDistanceCell(re, tile))

      Some(tile)
    }
  }

  /**
   * Computes a tiled Euclidean distance raster over a very large, dense point set.
   *
   * Given a very large, dense point set—on the order of 10s to 100s of millions
   * of points or more—this apply method provides the means to relatively
   * efficiently produce a Euclidean distance raster layer for those points.
   * This operator assumes that there is a layout definition for the space in
   * qustion, and that the input points have been binned according to that
   * layout's mapTransform object.
   *
   * There are caveats to using this function.  The assumption is that the
   * incoming point set is _dense_ relative to the layout.  An empty SpatialKey
   * in isolation is not probematic.  However, large areas devoid of points
   * which translate to many adjacent spatial keys in the grid which are empty
   * will likely cause problems.  Additionally, tiles whose Euclidean distance
   * values are defined by points that are not in the current tile or in one of
   * the 8 neighboring tiles will produce incorrect values.  This is
   * particularly true in regions outside the mass of points where the medial
   * axis of the point set intrudes (these will be generated in large interior
   * voids or in areas where the point set boundary has nonconvex pockets).
   */
  def apply(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition): RDD[(SpatialKey, Tile)] = {
    val triangulations: RDD[(SpatialKey, DelaunayTriangulation)] =
      rdd
        .map { case (key, points) =>
          (key, DelaunayTriangulation(points))
        }

    val borders: RDD[(SpatialKey, BoundaryDelaunay)] =
      triangulations
        .mapPartitions({ iter =>
          iter.map{ case (sk, dt) => {
            val ex: Extent = layoutDefinition.mapTransform(sk)
            (sk, BoundaryDelaunay(dt, ex))
          }}
        }, preservesPartitioning = true)

    borders
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.map { case (key, neighbors) =>
          val newNeighbors =
            neighbors.map { case (direction, (key2, border)) =>
              val ex = layoutDefinition.mapTransform(key2)
              (direction, (border, ex))
            }
          (key, newNeighbors.toMap)
        }
      }, preservesPartitioning = true)
      .join(triangulations)
      .mapPartitions({ partition =>
        partition.flatMap { case (key, (borders, triangulation)) => { // : (Map[Direction, (BoundaryDelaunay, Extent)], DelaunayTriangulation)
          val extent = layoutDefinition.mapTransform(key)
          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          neighborEuclideanDistance(triangulation, borders, re) match {
            case None => None
            case Some(tile) => Some(key, tile)
          }
        }}
      }, preservesPartitioning = true)

  }

}

object SparseEuclideanDistance {

  /**
   * Generates a Euclidean distance tile RDD from a small collection of points.
   *
   * The standard EuclideanDistance object apply method is meant to build
   * Euclidean distance tiles from a very large, dense set of points where the
   * generation of the triangulation itself is a significant performance
   * bottleneck (i.e., when the point set is 10s or 100s of millions strong or
   * more).  In the event that one wishes to generate a set of Euclidean
   * distance tiles over a large area from a small or sparse set of points which
   * would exhibit artifacts under the assumptions employed by the dense
   * EuclideanDistance operator, this apply method will accomplish the desired
   * end.  Be aware, however, that if there are many points, this operation will
   * have a lengthy runtime.
   */
  def apply(pts: Array[Coordinate], 
            geomExtent: Extent, 
            ld: LayoutDefinition, 
            tileCols: Int,
            tileRows: Int,
            cellType: CellType = DoubleConstantNoDataCellType)(implicit sc: SparkContext): RDD[(SpatialKey, Tile)] = {
    val dt = sc.broadcast(DelaunayTriangulation(pts))
    val GridBounds(cmin, rmin, cmax, rmax) = ld.mapTransform(geomExtent)
    val keys = sc.parallelize(for (r <- rmin to rmax; c <- cmin to cmax) yield SpatialKey(c, r))

    keys.mapPartitions(_.map{ key =>
      val ex = ld.mapTransform(key)
      val vor = new VoronoiDiagram(dt.value, ex)
      val re = RasterExtent(ex, tileCols, tileRows)
      val tile = ArrayTile.empty(cellType, re.cols, re.rows)

      vor.voronoiCellsWithPoints.foreach(EuclideanDistanceTile.rasterizeDistanceCell(re, tile))

      (key, tile)
    }, preservesPartitioning=true)
  }

}
