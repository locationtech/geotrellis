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
    fn: FractionCallback
  ): Unit = {
    // Screen coordinates
    val (x0, y0, x1, y1) = edge
    val xmin = min(x0, x1)
    val ymin = min(y0, y1)
    val xmax = max(x0, x1)
    val ymax = max(y0, y1)
    val m = (y1 - y0) / (x1 - x0)

    // Integral screen coordinates
    val xminint = floor(xmin).toInt
    val yminint = floor(ymin).toInt
    val xmaxint =  ceil(xmax).toInt
    val ymaxint =  ceil(ymax).toInt

    // Map coordinates
    val xminmap = re.gridColToMap(xminint) - re.cellwidth/2
    val yminmap = re.gridRowToMap(ymaxint) + re.cellheight/2
    val xmaxmap = re.gridColToMap(xmaxint) - re.cellwidth/2
    val ymaxmap = re.gridRowToMap(yminint) + re.cellheight/2

    // Envelope around the edge (in map space)
    val envelope = Polygon(
      Point(xminmap, yminmap),
      Point(xminmap, ymaxmap),
      Point(xmaxmap, ymaxmap),
      Point(xmaxmap, yminmap),
      Point(xminmap, yminmap)
    ).jtsGeom

    // Intersection of envelope and polygon (in map space)
    val localPoly = poly.jtsGeom.intersection(envelope)

    if (abs(m) <= 1) { // Mostly horizontal
      var x = xminint; while (x <= xmaxint) {
        val _y = floor(m * (x + 0.5 - x0) + y0).toInt
        var i = -1; while (i <= 1) {
          val y = _y + i
          val pair = (x, y)
          val xmap0 = re.gridColToMap(x+0) - re.cellwidth/2
          val xmap1 = re.gridColToMap(x+1) - re.cellwidth/2
          val ymap0 = re.gridRowToMap(y+0) + re.cellheight/2
          val ymap1 = re.gridRowToMap(y+1) + re.cellheight/2
          val pixel = Polygon(
            Point(xmap0, ymap0),
            Point(xmap0, ymap1),
            Point(xmap1, ymap1),
            Point(xmap1, ymap0),
            Point(xmap0, ymap0)
          ).jtsGeom
          val fraction = (pixel.intersection(localPoly)).getArea / pixel.getArea

          if (fraction > 0.0) {
            synchronized {
              if (!set.contains(pair)) {
                fn(x, y, fraction)
                set += ((x, y))
              }
            }
          }
          i += 1
        }
        x += 1
      }
    } else { // Mostly vertical
      val m = (x1 - x0) / (y1 - y0)
      var y = yminint; while (y <= ymaxint) {
        val _x = floor(m * (y + 0.5 - y0) + x0).toInt
        var i = -1; while (i <= 1) {
          val x = _x + i
          val pair = (x, y)
          val xmap0 = re.gridColToMap(x+0) - re.cellwidth/2
          val xmap1 = re.gridColToMap(x+1) - re.cellwidth/2
          val ymap0 = re.gridRowToMap(y+0) + re.cellheight/2
          val ymap1 = re.gridRowToMap(y+1) + re.cellheight/2
          val pixel = Polygon(
            Point(xmap0, ymap0),
            Point(xmap0, ymap1),
            Point(xmap1, ymap1),
            Point(xmap1, ymap0),
            Point(xmap0, ymap0)
          ).jtsGeom
          val fraction = (pixel.intersection(localPoly)).getArea / pixel.getArea

          if (fraction > 0.0) {
            synchronized {
              if (!set.contains(pair)) {
                fn(x, y, fraction)
                set += ((x, y))
              }
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
  )(fn: FractionCallback): Unit = {
    val seen = mutable.Set.empty[(Int, Int)]
    val option = Rasterizer.Options(includePartial = false, sampleType = PixelIsArea)

    polygonToEdges(poly, re)
      .par
      .foreach({ edge => renderEdge(edge, re, poly, seen, fn) })

    PolygonRasterizer.foreachCellByPolygon(poly, re) {(col: Int, row: Int) =>
      val pair = (col, row)
      if (!seen.contains(pair)) fn(col, row, 1.0)
    }
  }

}
