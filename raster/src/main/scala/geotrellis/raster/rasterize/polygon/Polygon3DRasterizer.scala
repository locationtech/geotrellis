package geotrellis.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.raster.rasterize.triangles._
import geotrellis.vector._
import geotrellis.vector.voronoi._

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.index.strtree.STRtree
import spire.syntax.cfor._

import scala.collection.JavaConverters._
import scala.collection.mutable

object Polygon3DRasterizer {
  private def polygonToEdges(poly: Polygon): Seq[Line] = {

    val arrayBuffer = mutable.ArrayBuffer.empty[Line]

    /** Find the outer ring's segments */
    val coords = poly.jtsGeom.getExteriorRing.getCoordinates
    cfor(1)(_ < coords.length, _ + 1) { ci =>
      val coord1 = coords(ci - 1)
      val coord2 = coords(ci)
      val segment = Line(Point(coord1.x, coord1.y), Point(coord2.x, coord2.y))

      arrayBuffer += segment
    }

    /** Find the segments for the holes */
    cfor(0)(_ < poly.numberOfHoles, _ + 1) { i =>
      val coords = poly.jtsGeom.getInteriorRingN(i).getCoordinates
      cfor(1)(_ < coords.length, _ + 1) { ci =>
        val coord1 = coords(ci - 1)
        val coord2 = coords(ci)
        val segment = Line(Point(coord1.x, coord1.y), Point(coord2.x, coord2.y))

        arrayBuffer += segment
      }
    }

    arrayBuffer
  }

  def rasterizePolygon(geom: jts.Polygon, tile: MutableArrayTile, re: RasterExtent): Unit = {
    val constraints = polygonToEdges(geom)
    val coordinates = geom.getCoordinates
    val points = coordinates.map({ coord => Point(coord.x, coord.y) })
    val dt = ConformingDelaunay(points, constraints)
    val triangles = dt.triangles.filter({ triangle => geom.contains(triangle) })
    val steinerPoints = dt.steinerPoints

    /**
      * Insert constraints into search tree.  It is assumed that all
      * Steiner points will be generated on constraints.
      */
    val rtree = new STRtree
    constraints.foreach({ line =>
      val a = line.vertices(0)
      val b = line.vertices(1)
      val xmin = math.min(a.x, b.x)
      val xmax = math.max(a.x, b.x)
      val ymin = math.min(a.y, b.y)
      val ymax = math.max(a.y, b.y)
      val envelope = new jts.Envelope(xmin, xmax, ymin, ymax)
      rtree.insert(envelope, line)
    })

    /** Build index map */
    val indexMap: Map[(Double, Double), Int] = (points ++ steinerPoints)
      .zipWithIndex
      .map({ case (point, index) =>
        (point.x, point.y) -> index })
      .toMap

    /** z-coordinates of original vertices */
    val zs1 = geom.getCoordinates.map({ coord => coord.z })

    /** z-coordinates of Steiner vertices */
    val zs2 = steinerPoints.map({ point =>
      val envelope = new jts.Envelope(point.x, point.x, point.y, point.y)
      val lines = rtree.query(envelope).asScala; require(lines.length > 0)
      val line =
        lines
          .map({ line => line.asInstanceOf[Line] })
          .map({ line => (line.distance(point), line) })
          .reduce({ (pair1, pair2) => if (pair1._1 <= pair2._1) pair1; else pair2 })
          ._2
      val a = line.vertices(0) // first endpoint
      val aIndex = indexMap.getOrElse((a.x, a.y), throw new Exception) // index of first endpoint
      val aValue = zs1(aIndex) // z value at first endpoint
      val b = line.vertices(1) // second endpoint
      val bIndex = indexMap.getOrElse((b.x, b.y), throw new Exception) // index of second endpoint
      val bValue = zs1(bIndex) // z value at second endpoint
      val dista = a.distance(point) // distance from Steiner point to first endpoint
      val distb = b.distance(point) // distance from Steiner point  to second endpoint
      val t = distb / (dista + distb) // the proportion of the interpolated value that should come from the first endpoint

      t*aValue + (1-t)*bValue }) // the interpolated value
      .toArray

    /** Build source array */
    val sourceArray: Array[Double] = zs1 ++ zs2

    TrianglesRasterizer(re, tile, sourceArray, triangles, indexMap)
  }

  def rasterize(geom: jts.Geometry, re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType): Raster[Tile] = {
    val tile = ArrayTile.empty(cellType, re.cols, re.rows)

    rasterize(tile, geom, re)

    Raster(tile, re.extent)
  }

  def rasterize(tile: MutableArrayTile, geom: jts.Geometry, re: RasterExtent): Unit = {
    val extentGeom = re.extent.toPolygon.jtsGeom
    geom match {
      case p: jts.Polygon =>
        if(p.intersects(extentGeom)) {
          rasterizePolygon(p, tile, re)
        }
      case mp: jts.MultiPolygon =>
        for(p <- MultiPolygon(mp).polygons) {
          rasterizePolygon(p.jtsGeom, tile, re)
        }
    }
  }
}
