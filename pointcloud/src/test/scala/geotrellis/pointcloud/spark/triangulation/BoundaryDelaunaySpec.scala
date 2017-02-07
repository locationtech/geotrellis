package geotrellis.pointcloud.spark.triangulation

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import com.vividsolutions.jts.geom.Coordinate
import geotrellis.vector.Extent
import geotrellis.vector.triangulation._
import org.scalatest.{FunSpec, Matchers}

import scala.util.Random

class TEMPBoundaryDelaunaySpec extends FunSpec with Matchers {
import io.pdal._
import geotrellis.raster._
import geotrellis.raster.rasterize.triangles.TrianglesRasterizer
import geotrellis.raster.io.geotiff._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.spark.buffer.Direction
import geotrellis.spark.io._
import geotrellis.pointcloud.spark.io.hadoop.HadoopPointCloudRDD
import geotrellis.spark.io.file._
import geotrellis.pointcloud.pipeline._
import geotrellis.pointcloud.spark._
import geotrellis.pointcloud.spark.io._
import geotrellis.pointcloud.spark.dem._
import geotrellis.pointcloud.spark.tiling._
import geotrellis.pointcloud.spark.triangulation._
import geotrellis.pointcloud.spark.json._
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.spark.io.index._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.triangulation._
import geotrellis.proj4._
import geotrellis.util._
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import spire.syntax.cfor._
import spray.json._
import com.vividsolutions.jts.geom.Coordinate
import geotrellis.raster.triangulation.DelaunayRasterizer

import scala.collection.mutable
import scala.collection.JavaConverters._

  val stitchPath =
    "/Users/rob/proj/jets/las/Goonyella_14B_20160913_AMG66z55.las"

  def time[T](msg: String)(f: => T) = {
    val start = System.currentTimeMillis
    val v = f
    val end = System.currentTimeMillis
    println(s"[TIMING] $msg: ${java.text.NumberFormat.getIntegerInstance.format(end - start)} ms")
    v
  }


  def doBoundaryStitch(len: Int): Unit = {
    val dir = s"/Users/rob/proj/jets/14B-${len*len}tiles"
    val saveDir = {
      val p = s"/Users/rob/proj/jets/borderTest-14B-${len*len}tiles"
      val d = new java.io.File(p)
      if(!d.exists) { d.mkdir }
      p
    }

    //    val path = d8Path
    val path = stitchPath
    def targetPath(col: Int, row: Int) =
      s"${dir}/Goonyella_14B_09_${col}_${row}.las"

    def savePath(k: SpatialKey) =
      s"${saveDir}/border-TIN-${k.col}-${k.row}.tif"

//    val coordsToInclude: Set[(Int, Int)] = Set((0,0), (1, 0), (0, 1), (1, 1))
    val coordsToInclude: Set[(Int, Int)] =
      Set(
        (9, 7),
        (9, 8),
        (9, 6),
        (8, 7),
        (8, 8),
        (8, 6)
      )

    def filter(col: Int, row: Int): Boolean = {
      coordsToInclude.contains((col, row))
//      true
    }

    val pointClouds: Seq[(SpatialKey, (PointCloud, Extent))] =
      (for(col <- 0 until len;
        row <- 0 until len) yield {
        if(filter(col, row)) {
          val path = targetPath(col, row)
          val (extent, pointClouds) =
            time(s"Read point clouds - $path") {
              val pipeline = Pipeline(Read(new java.io.File(path).toString): PipelineConstructor)
              pipeline.execute
              val extent = (new PointCloudHeader {
                val metadata = pipeline.getMetadata()
                val schema = ""
              }).extent
              val pointViewIterator = pipeline.getPointViews()
              val result =
                pointViewIterator.asScala.toList.map { pointView =>
                  val pointCloud =
                    pointView.getPointCloud

                  pointView.dispose()
                  pointCloud
                }.toIterator
              pointViewIterator.dispose()
              pipeline.dispose()
              (extent, result)
            }

          Some((SpatialKey(col, row), (pointClouds.next, extent)))
        } else { None }

      }).flatten

    def createDelaunay(pc: PointCloud): DelaunayTriangulation = {
      var j = 0
      val pointsFull = {
        val arr = Array.ofDim[Coordinate](pc.length)
        cfor(0)(_ < pc.length, _ + 1) { i =>
          val z = pc.getZ(i)
          //              if(z < 400) {
          val x = pc.getX(i)
          val y = pc.getY(i)

          arr(j) = (new Coordinate(x, y, z))
          j += 1
          //              }
        }

        arr
      }

      println(s"--${pointsFull.size} POINTS--")

      val points = Array.ofDim[Coordinate](j)
      j -= 1
      while(j >= 0) {
        points(j) = pointsFull(j)
        j -= 1
      }


      DelaunayTriangulation(points, debug = false)
    }

    def collectNeighbors[V](seq: Seq[(SpatialKey, V)]): Map[SpatialKey, Iterable[(Direction, (SpatialKey, V))]] = {
      import Direction._

      seq
        .flatMap { case (key, value) =>
          val SpatialKey(col, row) = key

          Seq[(SpatialKey, (Direction, (SpatialKey, V)))](
            (key, (Center, (key, value))),

            (SpatialKey(col-1, row), (Right, (key, value))),
            (SpatialKey(col+1, row), (Left, (key, value))),
            (SpatialKey(col, row-1), (Bottom, (key, value))),
            (SpatialKey(col, row+1), (Top, (key, value))),

            (SpatialKey(col-1, row-1), (BottomRight, (key, value))),
            (SpatialKey(col+1, row-1), (BottomLeft, (key, value))),
            (SpatialKey(col-1, row+1), (TopRight, (key, value))),
            (SpatialKey(col+1, row+1), (TopLeft, (key, value)))
          )
        }
        .groupBy(_._1)
        .map { case (k, v) => (k, v.map(_._2)) }
        .filter { case (_, values) =>
          values.find {
            case (Center, _) => true
            case _ => false
          }.isDefined
        }
    }

    implicit class withSeqJoinMethods[K, V](val self: Seq[(K, V)]) {
      def join[U](other: Seq[(K, U)]): Seq[(K, (V, U))] = {
        val m1 = self.toMap
        val m2 = other.toMap
        (for(k <- m1.keys.toSet.intersect(m2.keys.toSet)) yield {
          (k, (m1(k), m2(k)))
        }).toSeq
      }
    }

    val delaunays = pointClouds.map { case (k, (v, e)) =>
      time(s"|$k| - creating delaunay") {
        (k, (createDelaunay(v), e))
      }
    }

    val borders =
      delaunays.map { case (k, (delaunay, e)) =>
        time(s"|$k| - creating border delaunay") {
          (k, (BoundaryDelaunay(delaunay, e), e))
        }
      }

    val saveWktDir = {
      val p = s"/Users/rob/proj/jets/borderTest-14B-WKT-${len*len}tiles"
      val d = new java.io.File(p)
      if(!d.exists) { d.mkdir }
      p
    }

    def saveWktPath(k: SpatialKey) =
      s"${saveWktDir}/border-TIN-${k.col}-${k.row}.wkt"

    def saveWkt(fname: String, g: Geometry) = {
      val wktString = geotrellis.vector.io.wkt.WKT.write(g)
      val path = s"${saveWktDir}/${fname}.wkt"
      new java.io.PrintWriter(path) { write(wktString); close }
    }

    // for((k, (border, e)) <- borders) {
    //   time(s"|$k| - saving border WKT") {
    //     border.writeWkt(saveWktPath(k))
    //   }
    // }

    val fromVertices = for((k, (border, e)) <- borders) yield {
      border.trianglesFromVertices
    }

    saveWkt("all-from-verts", GeometryCollection(multiPolygons = fromVertices.toSeq))

    // val tiles: Seq[(SpatialKey, Raster[Tile])] =
    //   collectNeighbors(borders)
    //     .map { case (k, neighbors) =>
    //       val nn =
    //         neighbors.map { case (direction, (_, (border, ex))) =>
    //           (direction, (border, ex))
    //         }
    //       (k, nn)
    //     }
    //     .toSeq
    //     .join(delaunays)
    //     .map { case (k, (borders, (triangulation, extent))) =>
    //       println(s"STITCHING AND RASTERIZING $k")
    //       val stitched =
    //         time(s"|$k| - stitching delaunay from borders") {
    //           StitchedDelaunay(borders.toMap)
    //         }

    //       val re =
    //         RasterExtent(
    //           extent,
    //           256,
    //           256
    //         )

    //       val tile =
    //         time(s"|$k| - stitched rasterize") {
    //           stitched.rasterize(re, DoubleConstantNoDataCellType)(triangulation)
    //         }

    //       (k, Raster(tile, extent))
    //     }

    // for((k, raster) <- tiles) {
    //   time(s"|$k| - saving tile") {
    //     GeoTiff(raster.tile, raster.extent, WebMercator)
    //       .write(savePath(k))
    //   }

    // }
  }

  describe("TEMPBoundaryDelaunay") {
    it("should do stuff") {
      doBoundaryStitch(10)
    }
  }
}

class BoundaryDelaunaySpec extends FunSpec with Matchers {

  def randInRange(low: Double, high: Double): Double = {
    val x = Random.nextDouble
    low * (1-x) + high * x
  }

  def randomPoint(extent: Extent): Coordinate = {
    new Coordinate(randInRange(extent.xmin, extent.xmax), randInRange(extent.ymin, extent.ymax))
  }

  def randomizedGrid(n: Int, extent: Extent): Seq[Coordinate] = {
    val xs = (for (i <- 1 to n) yield randInRange(extent.xmin, extent.xmax)).sorted
    val ys = for (i <- 1 to n*n) yield randInRange(extent.ymin, extent.ymax)

    xs.flatMap{ x => {
      val yvals = Random.shuffle(ys).take(n).sorted
      yvals.map{ y => new Coordinate(x, y) }
    }}
  }

  describe("BoundaryDelaunay") {
    it("should take all triangles with circumcircles outside extent") {
      val ex = Extent(0,0,1,1)
      val pts = (for ( i <- 1 to 1000 ) yield randomPoint(ex)).toArray
      val dt = DelaunayTriangulation(pts)
      val bdt = BoundaryDelaunay(dt, ex)
      val bdtTris = bdt.triangleMap.getTriangles.keys.toSet

      def circumcircleLeavesExtent(tri: Int): Boolean = {
        import dt.halfEdgeTable._
        import dt.pointSet._
        import dt.predicates._

        val center = circleCenter(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri))))
        val radius = center.distance(getCoordinate(getDest(tri)))
        val ppd = new PointPairDistance

        DistanceToPoint.computeDistance(ex.toPolygon.jtsGeom, center, ppd)
        ppd.getDistance < radius
      }

      dt.triangleMap.getTriangles.toSeq.forall{ case (idx, tri) => {
        if (circumcircleLeavesExtent(tri))
          bdtTris.contains(idx)
        else {
          //!bdtTris.contains(idx)
          true
        }
      }} should be (true)
    }

    it("should have sane triangle ordering near boundaries") {
      val pts = randomizedGrid(300, Extent(0,0,1,1)).toArray
      val dt = DelaunayTriangulation(pts, false)
      val bdt = BoundaryDelaunay(dt, Extent(0,0,1,1))

      // implicit val trans = { i: Int => pts(i) }
      import bdt.halfEdgeTable._
      val predicates = new Predicates(bdt.pointSet, bdt.halfEdgeTable)
      import predicates._

      var validCW = true
      var e = bdt.boundary
      do {
        var f = e
        do {
          if (rotCWSrc(f) != e)
            validCW = !isLeftOf(f, getDest(rotCWSrc(f)))

          f = rotCWSrc(f)
        } while (validCW && f != e)

        e = getNext(e)
      } while (validCW && e != bdt.boundary)

      var validCCW = true
      e = bdt.boundary
      do {
        var f = getFlip(e)
        do {
          if (rotCCWSrc(f) != getFlip(e))
            validCCW = !isRightOf(f, getDest(rotCCWSrc(f)))

          f = rotCCWSrc(f)
        } while (validCCW && f != getFlip(e))

        e = getNext(e)
      } while (validCCW && e != bdt.boundary)

      (validCW && validCCW) should be (true)
    }
  }

}
