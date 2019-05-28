package geotrellis.spark.distance

import geotrellis.proj4._
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.buffer.Direction
import geotrellis.raster.buffer.Direction._
import geotrellis.raster.distance.{EuclideanDistanceTile => RasterEuclideanDistance}
import geotrellis.raster.render._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.io.wkt.WKT

import org.locationtech.jts.geom.Coordinate

import org.apache.spark.rdd.RDD

import scala.util.Random
import scala.math.{Pi, atan, cos, max, pow, sin}

import org.scalatest._

import spire.syntax.cfor._


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
    while (site.getZ < 0) site = proposal

    while (i < n) {
      val next = proposal
      if (next.getZ > site.getZ || Random.nextDouble < next.getZ / site.getZ) {
        // if (next.getZ > site.getZ)
        //   print("↑")
        // else
        //   print("↓")
        sample(i) = next
        site = next
        i += 1
      } else {
        // if (next.getZ < 0)
        //   print("☠")
        // else
        //   print("-")
      }
    }
    println

    sample
  }

  describe("Distributed Euclidean distance") {

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
      // val maxDistance = rasterTile.findMinMaxDouble._2 + 1e-8
      // val cm = ColorMap((0.0 to maxDistance by (maxDistance/512)).toArray, ColorRamps.BlueToRed)
      // rasterTile.renderPng(cm).write("base_distance.png")

      println("  Forming stitched EuclideanDistance tile")
      val neighborTile = EuclideanDistance.neighborEuclideanDistance(center, bounds, rasterExtent).get
      // neighborTile.renderPng(cm).write("spark_distance.png")
      println("  Finished")

      assertEqual(neighborTile, rasterTile)
    }

    it("should work in a spark environment") {
      // val domain = Extent(-1.0, -0.5, 1.0, 1.0)
      val domain = Extent(0, -1.15, 1, -0.05)
      val sample = generatePoints(domain, 2500)

      // val wktString = scala.io.Source.fromFile("euclidean_distance_sample.wkt").getLines.mkString
      // val sample = geotrellis.vector.io.wkt.WKT.read(wktString).asInstanceOf[MultiPoint].points.map(_.jtsGeom.getCoordinate)

      val rasterExtent = RasterExtent(domain, 1024, 1024)
      val layoutdef = LayoutDefinition(rasterExtent, 256, 256)
      val maptrans = layoutdef.mapTransform

      // Chunk up sample into Map[SpatialKey, Array[Coordinate]], leaving only a single point in SpatialKey(1, 3)
      val broken = { val init = sample.groupBy{coord => maptrans(coord.x, coord.y)} ; init + ((SpatialKey(1,3), init(SpatialKey(1,3)).take(1))) }

      // def nbhdAround(key: SpatialKey, tiled: Map[SpatialKey, Array[Coordinate]]): Map[Direction, (SpatialKey, Array[Coordinate])] = {
      //   def skdist(base: SpatialKey)(other: SpatialKey) = {
      //     math.max(math.abs(base.col - other.col), math.abs(base.row - other.row))
      //   }

      //   tiled.filterKeys{ skdist(key)(_) <= 1 }.map{ case (k, coords) => {
      //     (k.col - key.col, k.row - key.row) match {
      //       case (0,0)   => (Center, (k, coords))
      //       case (-1,0)  => (Left, (k, coords))
      //       case (-1,1)  => (BottomLeft, (k, coords))
      //       case (0,1)   => (Bottom, (k, coords))
      //       case (1,1)   => (BottomRight, (k, coords))
      //       case (1,0)   => (Right, (k, coords))
      //       case (1,-1)  => (TopRight, (k, coords))
      //       case (0,-1)  => (Top, (k, coords))
      //       case (-1,-1) => (TopLeft, (k, coords))
      //     }
      //   }}
      // }

      // {
      //   val nbhd = nbhdAround(SpatialKey(1,0), broken)
      //   val dts = nbhd.map{ case (dir, (key, pts)) => (dir, (key, DelaunayTriangulation(pts))) }
      //   val bdts = dts.map{ case (dir, (key, dt)) => (dir, (BoundaryDelaunay(dt, maptrans(key)), maptrans(key))) }
      //   val (centerkey, center) = dts(Center)
      //   val stitched = StitchedDelaunay(center, bdts, false)
      //   println(s"Center boundary (id=${center.boundary}): [${center.halfEdgeTable.getSrc(center.boundary)} -> ${center.halfEdgeTable.getDest(center.boundary)}]")
      //   stitched.halfEdgeTable.navigate(center.boundary, stitched.indexToCoord, Map.empty)
      // }

      val newsample = broken.map(_._2.toSeq).reduce(_ ++ _)
      val rasterTile = newsample.euclideanDistanceTile(rasterExtent)

      val rdd: RDD[(SpatialKey, Array[Coordinate])] = 
        sc.parallelize(broken.toSeq)
          .map{ case (key, iter) => (key, iter.toArray) }

      rdd.foreach{ case (key, arr) => println(s"$key has ${arr.length} coordinates") }

      val tileRDD: RDD[(SpatialKey, Tile)] = rdd.euclideanDistance(layoutdef)
      val stitched = tileRDD.stitch

      // For to export point data 
      // val mp = MultiPoint(newsample.map{ Point.jtsCoord2Point(_)})
      // val wktString = geotrellis.vector.io.wkt.WKT.write(mp)
      // new java.io.PrintWriter("euclidean_distance_sample.wkt") { write(wktString); close }

      // Image file output
      // val maxDistance = rasterTile.findMinMaxDouble._2 + 1e-8
      // val cm = ColorMap((0.0 to maxDistance by (maxDistance/512)).toArray, ColorRamps.BlueToRed)
      // rasterTile.renderPng(cm).write("distance.png")
      // stitched.renderPng(cm).write("stitched.png")

      assertEqual(rasterTile, stitched)
    }

    it("should work for zero- and one-point input partitions") {
      val points = Array(new Coordinate(0.5, 1.5), new Coordinate(1.5, 0.5), new Coordinate(2.5, 1.5), new Coordinate(1.5, 2.5))
      val dirs = Array(Left, Bottom, Right, Top)
      val extent = Extent(1, 1, 2, 2)
      val rasterExtent = RasterExtent(extent, 512, 512)

      def directionToExtent(dir: Direction): Extent = dir match {
        case Center =>      Extent(1, 1, 2, 2)
        case Left =>        Extent(0, 1, 1, 2)
        case BottomLeft =>  Extent(0, 0, 1, 1)
        case Bottom =>      Extent(1, 0, 2, 1)
        case BottomRight => Extent(2, 0, 3, 1)
        case Right =>       Extent(2, 1, 3, 2)
        case TopRight =>    Extent(2, 2, 3, 3)
        case Top =>         Extent(1, 2, 2, 3)
        case TopLeft =>     Extent(0, 2, 1, 3)
      }

      val keyedPoints: Seq[(Direction, Array[Coordinate])] =
        dirs.zip(points).map{ case (dir, pt) => (dir, Array(pt)) }

      println("Forming DelaunayTriangulations")
      val triangulations = keyedPoints.map{ case (dir, pts) => {
        (dir, DelaunayTriangulation(pts))
      }}

      println("Preparing input for stitching")
      val stitchInput =
        triangulations
          .map{ case (dir, dt) => {
            val ex = directionToExtent(dir)
            (dir, (BoundaryDelaunay(dt, ex), ex))
          }}
          .toMap

      println("Forming StitchedDelaunay")
      val stitch = StitchedDelaunay(DelaunayTriangulation(Array.empty[Coordinate]),
        stitchInput.map{ case(k,v) => (convertDirection(k), v)}, false)
      cfor(0)(_ < stitch.pointSet.length, _ + 1) { i =>
        println(s"${i}: ${stitch.pointSet.getCoordinate(i)}")
      }
      println(s"  Resulting triangles: ${stitch.triangles}")

      println(s"Rasterizing full point set")
      val baselineEDT = RasterEuclideanDistance(points, rasterExtent)
      println(s"Rasterizing stitched point set")
      val stitchedEDT = EuclideanDistance.neighborEuclideanDistance(DelaunayTriangulation(Array.empty[Coordinate]), stitchInput, rasterExtent).get
      println(s"Done!")

      assertEqual(baselineEDT, stitchedEDT)
    }

    it("should work for a linear stitch result") {
      val points = Array(new Coordinate(2.5, 0.5), new Coordinate(2.5, 2.5))
      val dirs = Array(BottomRight, TopRight)
      val extent = Extent(1, 1, 2, 2)
      val rasterExtent = RasterExtent(extent, 512, 512)

      def directionToExtent(dir: Direction): Extent = dir match {
        case Center =>      Extent(1, 1, 2, 2)
        case Left =>        Extent(0, 1, 1, 2)
        case BottomLeft =>  Extent(0, 0, 1, 1)
        case Bottom =>      Extent(1, 0, 2, 1)
        case BottomRight => Extent(2, 0, 3, 1)
        case Right =>       Extent(2, 1, 3, 2)
        case TopRight =>    Extent(2, 2, 3, 3)
        case Top =>         Extent(1, 2, 2, 3)
        case TopLeft =>     Extent(0, 2, 1, 3)
      }

      val keyedPoints: Seq[(Direction, Array[Coordinate])] =
        dirs.zip(points).map{ case (dir, pt) => (dir, Array(pt)) }

      println("Forming DelaunayTriangulations")
      val triangulations = keyedPoints.map{ case (dir, pts) => {
        (dir, DelaunayTriangulation(pts))
      }}

      println("Preparing input for stitching")
      val stitchInput =
        triangulations
          .map{ case (dir, dt) => {
            val ex = directionToExtent(dir)
            (dir, (BoundaryDelaunay(dt, ex), ex))
          }}
          .toMap

      println("Forming StitchedDelaunay")
      val stitch = StitchedDelaunay(stitchInput.map{ case(k,v) => (convertDirection(k), v)}, false)
      cfor(0)(_ < stitch.pointSet.length, _ + 1) { i =>
        println(s"${i}: ${stitch.pointSet.getCoordinate(i)}")
      }
      println(s"  Resulting triangles: ${stitch.triangles}")

      println(s"Rasterizing full point set")
      val baselineEDT = RasterEuclideanDistance(points, rasterExtent)
      println(s"Rasterizing stitched point set")
      val stitchedEDT = EuclideanDistance.neighborEuclideanDistance(DelaunayTriangulation(Array.empty[Coordinate]), stitchInput, rasterExtent).get
      println(s"Done!")

      assertEqual(baselineEDT, stitchedEDT)
    }

    it("SparseEuclideanDistance should produce correct results") {
      val geomWKT = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/wkt/schools.wkt")).getLines.mkString
      val geom = geotrellis.vector.io.wkt.WKT.read(geomWKT).asInstanceOf[MultiPoint]
      val coords = geom.points.map(_.jtsGeom.getCoordinate)

      val LayoutLevel(_, ld) = ZoomedLayoutScheme(WebMercator).levelForZoom(12)
      val maptrans = ld.mapTransform
      val gb @ GridBounds(cmin, rmin, cmax, rmax) = maptrans(geom.envelope)
      val extent = maptrans(gb)
      val rasterExtent = RasterExtent(extent, 256 * (cmax - cmin + 1), 256 * (rmax - rmin + 1))

      println(s"Loaded ${coords.size} points")
      println(s"$gb")

      println("Computing baseline Euclidean distance tile (raster package)")
      val baseline = RasterEuclideanDistance(coords, rasterExtent)
      println(s"    Baseline has size (${baseline.cols}, ${baseline.rows})")

      println("Computing sparse Euclidean distance (spark)")
      val stitched = SparseEuclideanDistance(coords, extent, ld, 256, 256).stitch
      println(s"    Stitched has size (${stitched.cols}, ${stitched.rows})")

      assertEqual(baseline, stitched)
    }

  }
}
