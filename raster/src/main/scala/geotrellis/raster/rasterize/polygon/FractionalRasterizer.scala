package geotrellis.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._

import com.vividsolutions.jts.geom.Envelope
import spire.syntax.cfor._

import scala.collection.mutable
import scala.math.{min, max, ceil, floor, abs}


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
        if (col1 < col2) (col1, row1, col2, row2)
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
          if (col1 < col2) (col1, row1, col2, row2)
          else (col2, row2, col1, row1)

        arrayBuffer += segment
      }
    }

    arrayBuffer
  }

  private def renderEdge(
    edge: Segment,
    re: RasterExtent,
    poly: Polygon,
    set: mutable.Set[(Int, Int)],
    cb: FractionCallback
  ): Unit = {
    val (x0, y0, x1, y1) = edge
    val m = (y1 - y0) / (x1 - x0)

    // Grid coordinates
    val colMin = floor(min(x0, x1)).toInt
    val rowMin = floor(min(y0, y1)).toInt
    val colMax =  ceil(max(x0, x1)).toInt
    val rowMax =  ceil(max(y0, y1)).toInt

    // Map coordinates
    val xmin = re.gridColToMap(colMin) - re.cellwidth/2
    val ymin = re.gridRowToMap(rowMax) + re.cellheight/2
    val xmax = re.gridColToMap(colMax) - re.cellwidth/2
    val ymax = re.gridRowToMap(rowMin) + re.cellheight/2

    // Envelope around the edge (in map space)
    val envelope = Polygon(
      Point(xmin, ymin),
      Point(xmin, ymax),
      Point(xmax, ymax),
      Point(xmax, ymin),
      Point(xmin, ymin)
    ).jtsGeom

    // Intersection of envelope and polygon (in map space)
    val localPoly = poly.jtsGeom.intersection(envelope)

    if (abs(m) <= 1) { // The edge is mostly horizontal
      var x = colMin; while (x <= colMax) {
        val _y = floor(m * (x + 0.5 - x0) + y0).toInt
        var i = -1; while (i <= 1) {
          val y = _y + i
          val pair = (x, y)
          val pixelMinX = re.gridColToMap(x+0) - re.cellwidth/2
          val pixelMaxX = re.gridColToMap(x+1) - re.cellwidth/2
          val pixelMinY = re.gridRowToMap(y+0) + re.cellheight/2
          val pixelMaxY = re.gridRowToMap(y+1) + re.cellheight/2
          val pixel = Polygon(
            Point(pixelMinX, pixelMinY),
            Point(pixelMinX, pixelMaxY),
            Point(pixelMaxX, pixelMaxY),
            Point(pixelMaxX, pixelMinY),
            Point(pixelMinX, pixelMinY)
          ).jtsGeom
          val fraction = (pixel.intersection(localPoly)).getArea / pixel.getArea

          if (fraction > 0.0) {
            if (!set.contains(pair)) {
              set += ((x, y))
              cb.callback(x, y, fraction)
            }
          }
          i += 1
        }
        x += 1
      }
    } else { // The edge is mostly vertical
      val m = (x1 - x0) / (y1 - y0)
      var y = rowMin; while (y <= rowMax) {
        val _x = floor(m * (y + 0.5 - y0) + x0).toInt
        var i = -1; while (i <= 1) {
          val x = _x + i
          val pair = (x, y)
          val pixelMinX = re.gridColToMap(x+0) - re.cellwidth/2
          val pixelMaxX = re.gridColToMap(x+1) - re.cellwidth/2
          val pixelMinY = re.gridRowToMap(y+0) + re.cellheight/2
          val pixelMaxY = re.gridRowToMap(y+1) + re.cellheight/2
          val pixel = Polygon(
            Point(pixelMinX, pixelMinY),
            Point(pixelMinX, pixelMaxY),
            Point(pixelMaxX, pixelMaxY),
            Point(pixelMaxX, pixelMinY),
            Point(pixelMinX, pixelMinY)
          ).jtsGeom
          val fraction = (pixel.intersection(localPoly)).getArea / pixel.getArea

          if (fraction > 0.0) {
            if (!set.contains(pair)) {
              set += ((x, y))
              cb.callback(x, y, fraction)
            }
          }
          i += 1
        }
        y += 1
      }
    }
  }

  def foreachCellByPolygon(
    poly: Polygon,
    re: RasterExtent
  )(cb: FractionCallback): Unit = {
    val seen = mutable.Set.empty[(Int, Int)]
    val option = Rasterizer.Options(includePartial = false, sampleType = PixelIsArea)

    polygonToEdges(poly, re)
      .foreach({ edge => renderEdge(edge, re, poly, seen, cb) })

    PolygonRasterizer.foreachCellByPolygon(poly, re) {(col: Int, row: Int) =>
      val pair = (col, row)
      if (!seen.contains(pair)) cb.callback(col, row, 1.0)
    }
  }

}
