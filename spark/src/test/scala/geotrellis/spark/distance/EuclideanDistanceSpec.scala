package geotrellis.spark.distance

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.raster._
import geotrellis.raster.distance.{EuclideanDistanceTile => RasterEuclideanDistance}
import geotrellis.raster.render._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.spark.distance.{EuclideanDistance => SparkEuclideanDistance}
import geotrellis.spark.tiling._
import geotrellis.spark.triangulation._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.io.wkt.WKT

import org.scalatest._

class EuclideanDistanceSpec extends FunSpec 
                            // with TestEnvironment
                            with Matchers 
                            with RasterMatchers {
  describe("Distributed Euclidean distance") {
    println("Starting distributed Euclidean distance tests ...") 
    it("should work for a real data set") {
      println("  Reading points")
      val wkt = getClass.getResourceAsStream("/wkt/excerpt.wkt")
      val wktString = scala.io.Source.fromInputStream(wkt).getLines.mkString
      val multiPoint = WKT.read(wktString).asInstanceOf[MultiPoint]
      val points: Array[Coordinate] = multiPoint.points.map(_.jtsGeom.getCoordinate)
      val fullExtent @ Extent(xmin, ymin, xmax, ymax) = multiPoint.envelope

      def keyToExtent(key: SpatialKey) = {
        val SpatialKey(col, row) = key
        val w = fullExtent.width / 3
        val h = fullExtent.height / 3

        Extent(xmin + w * col, ymax - h * (row + 1), xmin + w * (col + 1), ymax - h * row)
      }

      def keyToDirection(key: SpatialKey): Direction = key match {
        case SpatialKey(0, 0) => TopLeft
        case SpatialKey(1, 0) => Top
        case SpatialKey(2, 0) => TopRight
        case SpatialKey(0, 1) => Left
        case SpatialKey(1, 1) => Center
        case SpatialKey(2, 1) => Right
        case SpatialKey(0, 2) => BottomLeft
        case SpatialKey(1, 2) => Bottom
        case SpatialKey(2, 2) => BottomRight
      }

      println("  Building Delaunay triangulations")
      val triangulations = (for (x <- 0 to 2 ; y <- 0 to 2) yield SpatialKey(x, y)).toSeq.map { key =>
        val ex = keyToExtent(key)
        val pts = multiPoint.intersection(ex).asMultiPoint.get.points.map(_.jtsGeom.getCoordinate)
        val dt = DelaunayTriangulation(pts)
        (keyToDirection(key), (dt, ex))
      }.toMap
      println("  Extracting BoundaryDelaunay objects")
      val bounds = triangulations.mapValues{ case (dt, ex) => (BoundaryDelaunay(dt, ex), ex) }
      val (center, centerEx) = triangulations(Center)

      println("  Forming baseline EuclideanDistanceTile")
      val rasterExtent = RasterExtent(centerEx, 512, 512)
      val rasterTile = RasterEuclideanDistance(points, rasterExtent)
      val maxDistance = rasterTile.findMinMaxDouble._2 + 1e-8
      // val cm = ColorMap((0.0 to maxDistance by (maxDistance/512)).toArray, ColorRamps.BlueToRed)
      // rasterTile.renderPng(cm).write("base_distance.png")

      println("  Forming sparkified EuclideanDistance tile")
      val neighborTile = SparkEuclideanDistance.neighborEuclideanDistance(center, bounds, rasterExtent)
      // neighborTile.renderPng(cm).write("spark_distance.png")
      println("  Finished")

      assertEqual(neighborTile, rasterTile)
    }
  }
}
