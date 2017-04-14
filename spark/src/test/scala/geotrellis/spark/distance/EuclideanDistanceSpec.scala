package geotrellis.spark.distance

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.raster.distance.{EuclideanDistanceTile => RasterEuclideanDistance}
import geotrellis.raster.render._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.spark.testkit._
import geotrellis.spark.tiling._
import geotrellis.spark.triangulation._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.io.wkt.WKT

import scala.util.Random
import scala.math.{Pi, sin, cos, atan, max, pow}

import Implicits._

import org.scalatest._

class EuclideanDistanceSpec extends FunSpec 
                            with TestEnvironment
                            with Matchers 
                            with RasterMatchers {

  //def heightField(x: Double, y: Double): Double = math.pow(x*x + y*y - 1, 3) - x*x * y*y*y + 0.5

  //def heightField(x: Double, y: Double): Double = math.pow(math.sin(math.Pi * x) * math.cos(math.Pi * y), 2) - 0.1

  def heightField(x: Double, y: Double): Double = pow(max(0, sin(Pi*x) * atan(Pi*y) + 0.5) + max(0, 0.2 * sin(2*Pi*x) * cos(5*Pi*y)), 2)

  def generatePoints(ex: Extent, n: Int): Array[Coordinate] = {
    val Extent(xmin, ymin, _, _) = ex
    val w = ex.width
    val h = ex.height

    def proposal() = {
      val u = Random.nextDouble
      val v = Random.nextDouble
      val x = xmin + u * w
      val y = ymin + v * h

      new Coordinate(x, y, heightField(x, y))
    }

    val sample = Array.ofDim[Coordinate](n)
    var i = 0
    var site = proposal
    while (site.z < 0) site = proposal

    while (i < n) {
      val next = proposal
      if (next.z > site.z || Random.nextDouble < next.z / site.z) {
        // if (next.z > site.z)
        //   print("↑")
        // else
        //   print("↓")
        sample(i) = next
        site = next
        i += 1
      } else {
        // if (next.z < 0)
        //   print("☠")
        // else
        //   print("-")
      }
    }
    println

    sample
  }

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
      val neighborTile = EuclideanDistance.neighborEuclideanDistance(center, bounds, rasterExtent)
      // neighborTile.renderPng(cm).write("spark_distance.png")
      println("  Finished")

      assertEqual(neighborTile, rasterTile)
    }

    it("should work in a spark environment") {
      // val domain = Extent(-1.0, -0.5, 1.0, 1.0)
      val domain = Extent(0, -1.15, 1, -0.05)
      val sample = generatePoints(domain, 2500)
      
      val rasterExtent = RasterExtent(domain, 1024, 1024)
      val layoutdef = LayoutDefinition(rasterExtent, 256, 256)
      val maptrans = layoutdef.mapTransform

      val rasterTile = RasterEuclideanDistance(sample, rasterExtent)
      val rdd: RDD[(SpatialKey, Array[Coordinate])] = 
        sc.parallelize(sample.map{ coord => (maptrans(coord.x, coord.y), coord) })
          .groupByKey
          .map{ case (key, iter) => (key, iter.toArray) }

      rdd.foreach{ case (key, arr) => println(s"$key has ${arr.length} coordinates") }

      val tileRDD: RDD[(SpatialKey, Tile)] = rdd.euclideanDistance(layoutdef)
      val stitched = tileRDD.stitch

      // // For to export point data 
      // val mp = MultiPoint(sample.toSeq.map{ Point.jtsCoord2Point(_)})
      // val wktString = geotrellis.vector.io.wkt.WKT.write(mp)
      // new java.io.PrintWriter("euclidean_distance_sample.wkt") { write(wktString); close }

      // // Image file output
      // val maxDistance = rasterTile.findMinMaxDouble._2 + 1e-8
      // val cm = ColorMap((0.0 to maxDistance by (maxDistance/512)).toArray, ColorRamps.BlueToRed)
      // rasterTile.renderPng(cm).write("distance.png")
      // stitched.renderPng(cm).write("stitched.png")
      // geotrellis.raster.io.geotiff.GeoTiff(rasterTile, domain, geotrellis.proj4.LatLng).write("distance.tif")

      assertEqual(rasterTile, stitched)
    }
  }
}
