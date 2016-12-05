package geotrellis.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._

import com.vividsolutions.jts.geom.Envelope
import spire.syntax.cfor._

import scala.collection.mutable
import scala.math.{min, max, ceil, floor}


object FractionalRasterizer {

  private type Segment = (Double, Double, Double, Double)

  private def polygonToEdges(poly: Polygon, re: RasterExtent): Seq[Segment] = {

    val arrayBuffer = mutable.ArrayBuffer.empty[Segment]

    /** Find the outer ring's segments */
    val coords = poly.jtsGeom.getExteriorRing.getCoordinates
    cfor(1)(_ < coords.length, _ + 1) { ci =>
      val coord1 = coords(ci - 1)
      val coord2 = coords(ci)

      val col1 = re.mapXToGridDouble(coord1.x)
      val row1 = re.mapYToGridDouble(coord1.y)
      val col2 = re.mapXToGridDouble(coord2.x)
      val row2 = re.mapYToGridDouble(coord2.y)

      val segment =
        if (row1 < row2) (col1, row1, col2, row2)
        else (col2, row2, col1, row1)

      arrayBuffer += segment
    }

    /** Find the segments for the holes */
    cfor(0)(_ < poly.numberOfHoles, _ + 1) { i =>
      val coords = poly.jtsGeom.getInteriorRingN(i).getCoordinates
      cfor(1)(_ < coords.length, _ + 1) { ci =>
        val coord1 = coords(ci - 1)
        val coord2 = coords(ci)

        val col1 = re.mapXToGridDouble(coord1.x)
        val row1 = re.mapYToGridDouble(coord1.y)
        val col2 = re.mapXToGridDouble(coord2.x)
        val row2 = re.mapYToGridDouble(coord2.y)

        val segment =
          if (row1 < row2) (col1, row1, col2, row2)
          else (col2, row2, col1, row1)

        arrayBuffer += segment
      }
    }

    arrayBuffer
  }

  private def renderEdge(
    edge: Segment,
    poly: Polygon,
    set: mutable.Set[(Int, Int)],
    fn: FractionCallback
  ): Unit = {
    val xmin = floor(min(edge._1, edge._3)).toInt
    val ymin = floor(min(edge._2, edge._4)).toInt
    val xmax = ceil(max(edge._1, edge._3)).toInt
    val ymax = ceil(max(edge._2, edge._4)).toInt

    val envelope = Polygon(Point(xmin, ymin), Point(xmin, ymax), Point(xmax, ymax), Point(xmax, ymin), Point(xmin, ymin)).jtsGeom
    val localPoly = poly.jtsGeom.intersection(envelope)

    var x = xmin; while (x <= xmax) {
      var y = ymin; while (y <= ymax) {
        val pair = (x, y)
        if (!set.contains(pair)) {
          val pixel = Polygon(Point(x, y), Point(x, y+1), Point(x+1, y+1), Point(x+1, y), Point(x, y)).jtsGeom
          val fraction = (pixel.intersection(localPoly)).getArea

          if (fraction > 0.0) {
            fn(x, y, fraction)
            set += ((x, y))
          }
        }
        y += 1
      }
      x += 1
    }
  }

  def foreachCellByPolygon(
    poly: Polygon,
    re: RasterExtent
  )(fn: FractionCallback): Unit = {
    val seen = mutable.Set.empty[(Int, Int)]
    val option = Rasterizer.Options(includePartial = false, sampleType = PixelIsArea)

    polygonToEdges(poly, re).foreach({ edge => renderEdge(edge, poly, seen, fn) })

    PolygonRasterizer.foreachCellByPolygon(poly, re) {(col: Int, row: Int) =>
      val pair = (col, row)
      if (!seen.contains(pair)) fn(col, row, 1.0)
    }
  }

}
