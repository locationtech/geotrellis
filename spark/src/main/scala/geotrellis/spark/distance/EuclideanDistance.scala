package geotrellis.spark.distance

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.raster.distance._
import geotrellis.spark._
import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.spark.tiling._
import geotrellis.spark.triangulation._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.voronoi._

import scala.collection.mutable.{ListBuffer, Set}

object EuclideanDistance {

  def voronoiCells(centerStitched: StitchedDelaunay, initialVert: Int, extent: Extent): Seq[(Polygon, Coordinate)] = {
    import centerStitched.halfEdgeTable._

    val queue = ListBuffer[(Int, Int)]((edgeIncidentTo(initialVert), initialVert))
    val visited = Set.empty[Int]
    val result = ListBuffer.empty[(Polygon, Coordinate)]

    while (queue nonEmpty) {
      val (incoming, here) = queue.remove(0)

      if (!visited.contains(here)) {
        visited += here

        // println(s"    Generating cell for vertex $here")
        val poly = VoronoiDiagram.polygonalCell(centerStitched.halfEdgeTable, centerStitched.indexToCoord, extent)(incoming)

        if (poly isDefined) {
          // getDest(incoming) has a Voronoi cell intersecting the extent; add it to the cell complex and recurse to the neighbors
          result += ((poly.get, centerStitched.indexToCoord(here)))
          var e = getFlip(incoming)
          do {
            // println(s"    Queueing vertex ${getDest(e)}")
            queue += ((e, getDest(e)))
            e = rotCWSrc(e)
          } while (e != getFlip(incoming))
        }
      }
    }

    result
  }

  def neighborEuclideanDistance(center: DelaunayTriangulation, neighbors: Map[Direction, (BoundaryDelaunay, Extent)], re: RasterExtent): Tile = {
    val stitched = StitchedDelaunay(center, neighbors, false)
    val origin = center.halfEdgeTable.getDest(center.boundary)
    val extent = neighbors(Center)._2
    val cells = voronoiCells(stitched, origin, extent)

    val tile = DoubleArrayTile.empty(re.cols, re.rows)
    cells.foreach(EuclideanDistanceTile.rasterizeDistanceCell(re, tile))

    tile
  }

  def apply(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition): RDD[(SpatialKey, Tile)] = {
    val extent = layoutDefinition.extent
    val mapTransform = layoutDefinition.mapTransform

    val triangulations: RDD[(SpatialKey, DelaunayTriangulation)] =
      rdd
        .map { case (key, points) =>
          (key, DelaunayTriangulation(points))
        }

    val borders: RDD[(SpatialKey, BoundaryDelaunay)] =
      triangulations
        .mapPartitions{ iter =>
          iter.map{ case (sk, dt) => {
            val ex: Extent = layoutDefinition.mapTransform(sk)
            (sk, BoundaryDelaunay(dt, ex))
          }}
        }

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
        partition.map { case (key, (borders, triangulation)) => { // : (Map[Direction, (BoundaryDelaunay, Extent)], DelaunayTriangulation)
          val extent = layoutDefinition.mapTransform(key)
          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          (key, neighborEuclideanDistance(triangulation, borders, re))
        }}
      }, preservesPartitioning = true)

  }

}
