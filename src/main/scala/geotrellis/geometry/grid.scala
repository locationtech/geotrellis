package geotrellis.geometry.grid

import geotrellis._

import scala.util.Sorting
import math.{abs,min,max,round}

/** this represents an (x,y) point coordinate. arguably we should just use an
  * (Int, Int) tuple for this, but it's kind of nice to be able to do pt2.x
  * instead of pt2._1
  */
case class GridPoint(val x:Int, val y:Int) extends Ordered[GridPoint] {
  def compare(that:GridPoint) = {
    // if (this.y < that.y) -1
    // else if (that.y < this.y) 1
    // else if (this.x < that.x) - 1
    // else if (that.y < this.y) 1
    // else 0
    val cmp = this.y.compare(that.y)
    if (cmp != 0) {
      cmp
    } else {
      this.x.compare(that.x)
    }
  }

  def equal(that:GridPoint) = this.compare(that) == 0

  override def toString = "(%d, %d)".format(this.x, this.y)
}

/**
  * this represents a line segment from p1 -> p2. there are no assumptions about
  * p1's relationship to p2. we determine pmin (the point whose Y coordinate is
  * least (with lowest X coordinate used as a tie-breaker) and pmax (the
  * opposite) for when we need to know which is least or greatest.
  * we order lines by "lowest" pmin.
  */
case class GridLine(val p1:GridPoint, val p2:GridPoint) extends Ordered[GridLine] {
  // figure out which point has lowest Y (then X) coordinates
  var pmin:GridPoint = null
  var pmax:GridPoint = null
  if (p1.compare(p2) < 0) {
    pmin = p1
    pmax = p2
  } else {
    pmin = p2
    pmax = p1
  }

  // calculate the slope
  val dy     = p2.y - p1.y
  val dx     = p2.x - p1.x
  val yslope = dy.toDouble / dx
  val xslope = dx.toDouble / dy

  // see if we are dealing with a verticle or horizontal line
  val vertical   = dx == 0
  val horizontal = dy == 0

  override def toString = {
    "GridLine(%s -> %s)".format(this.p1, this.p2)
  }

  /**
    * compare two points based on minPoint (the upper/leftmost point).
    */
  def compare(that:GridLine) = {
    val cmp = this.pmin.compare(that.pmin)
    if (cmp != 0) {
      cmp
    } else {
      this.pmax.compare(that.pmax)
    }
  }

  def interceptsY(y:Int) = this.p1.y <= y && this.p2.y >= y || this.p2.y <= y && this.p1.y >= y

  def interceptsX(x:Int) = this.p1.x <= x && this.p2.x >= x || this.p2.x <= x && this.p1.x >= x

  /**
    * given a Y coordinate we should be able to calculate what the X coordinate
    * would be along this line. if the line was (0, 0) -> (4, 6), then getx(3)
    * would return 2.
    */
  def getx(y:Int) = {
    if (this.horizontal || this.vertical) {
      this.pmin.x
    } else {
      round(this.p1.x + (y - this.p1.y) * this.xslope).toInt
    }
  }
}

object GridPolygon {
  def apply(pts:Array[GridPoint]) = {
    assert(pts.length > 2)
    assert(pts(0).equal(pts(pts.length - 1)))

    import scala.collection.mutable.ArrayBuffer
    val edges = ArrayBuffer.empty[GridLine]

    // remove duplicate points.
    // TODO: move this code into Polygon.translateToGrid()
    var i = 0
    var j = 1
    val limit = pts.length
    while (j < limit) {
      val lst = pts(i)
      val nxt = pts(j)
      if (lst.compare(nxt) != 0) {
        edges.append(new GridLine(lst, nxt))
        i = j
        j += 1
      } else {
        j += 1
      }
    }
    //printf("BUILT %d EDGES\n", edges.length)

    if (edges.length <= 2) {
      //println(edges)
      throw new Exception("not enough edges!")
    }

    if (edges(0).p1 != edges.last.p2) {
      //println(edges(0).p1)
      //println(edges(0))
      //println(edges.last.p2)
      //println(edges.last)
      throw new Exception("invalid line string")
    }
    assert(edges(0).p1 == edges.last.p2)

    new GridPolygon(edges.toSeq)
  }
}
/**
  * a polygon is a set of lines. it is assumed that each line's p2 is equal to
  * the next line's p1 (with the final line's p2 equal to the first line's p1).
  * the user is required to provide lines in this order.
  */
case class GridPolygon(val edges:Seq[GridLine]) {
  val len = this.edges.length

  // check to make sure the lines are all attached correctly, and figure out
  // the bounding box around this polygon
  var xmin = 0
  var ymin = 0
  var xmax = 0
  var ymax = 0
  var i = 0
  while (i < len) {
    val edge = this.edges(i)
    val j    = (i + 1) % len
    if (!edge.p2.equal(this.edges(j).p1)) {
      //println(this.edges)
      throw new Exception("edges do not line up! %s".format(edges.toList))
    }
    xmin = min(xmin, min(edge.pmin.x, edge.pmax.x))
    xmax = max(xmax, max(edge.pmin.x, edge.pmax.x))
    ymin = min(ymin, edge.pmin.y)
    ymax = max(ymax, edge.pmax.y)
    i += 1
  }

  // // make sure indices are all positive
  // if(xmin < 0 || ymin < 0) {
  //   throw new Exception("negative indices not supported")
  // }

  def rasterize(raster:Raster, value:Int) {
    this.rasterize(raster.cols, raster.rows, value, raster.data, 0, 0)
  }
  /**
    * this uses a scanline-esque algorithm--we render a single row of pixels at
    * at time. to do this, we calculate the various X intercepts of each of our
    * line segments, and write between X intercepts. there are two special cases
    * that have to be handled: spikes and hinges.
    *
    * a spike looks like either: /\ or \/... it is a local Y-minima
    * (or Y-maxima). these are important because unlike other X coordinates they
    * do not indicate the start (or end) of a "filled region". only the pixel
    * itself should be filled.
    *
    * a hinge is a joint between two line segments whose slopes have the same
    * sign (e.g. they are both falling or rising). in this case, we only want
    * one of their X coordinates, not the other.
    */
  def rasterize(cols:Int, rows:Int, value:Int, _data:RasterData, xoffset:Int, yoffset:Int) {
    if (cols <= this.xmax || rows <= this.ymax) {
      val msg = "raster is not big enough (%d <= %d || %d <= %d)"
      throw new Exception(msg.format(cols, xmax, rows, ymax))
    }
  
    val data = _data.asArray

    // we sort our line segments by pmin (which is the upper (leftmost) point
    // on the line). we do this to make it easier to know when we need to start
    // worrying about a line segment.
    val lines = this.edges.toArray
    Sorting.quickSort(lines)

    var start = 0
    var limit = 0
    val len   = this.edges.length

    var row = 0
    while (row < rows) {

      // see if we have moved past some of our lines
      while (start < len && lines(start).pmax.y <  row) { start += 1 }
      while (limit < len && lines(limit).pmin.y <= row) { limit += 1 }

      // if the lower points of all of our line segemnts are higher than the
      // current row, then we know that we're done.
      ////if (start >= len) { return }

      // TODO: this whole thing probably needs optimization... instead of
      //       building tpls on each row we should probably just mutate the
      //       previous one. but that would be more complex probably.

      // first we create tuples (x, state) for each line we are considering.
      // x=-1 means that the line is not intersected. state=0 means that we are
      // talking about an edge that flows through this row, whereas state=1
      // means that the line starts on this row and state=2 means that the line
      // ends on this row. we need to use the start and end information to
      // find "spikes", which are treated differently.
      val tpls = (start until limit).toArray.map {
        i => {
          val line = lines(i)
          val ymin = line.pmin.y
          val ymax = line.pmax.y
          val tpl = if (ymax < row || ymin > row) {
            (-1, 0)
          } else if (line.horizontal) {
            (-1, 0)
          } else {
            val x = line.getx(row)

            if(ymin == row) {
              (x, 1)
            } else if (ymax == row) {
              (x, 2)
            } else {
              (x, 0)
            }
          }
          //Console.printf("%d: %s gave us %s\n", row, line, tpl)
          tpl
        }
      } filter { _._1 != -1 }
      Sorting.quickSort(tpls)

      // now we need to pull out spikes and hinges. a spike is a pair of tuples
      // who have the same X coordinate and the same state (meaning that two
      // lines either started or ended on this point). visually this looks like
      // a spike. two tuples with the same X coordinates represent a hinge
      // (where one line segment ends and another begins at a different angle).
      // other than those two cases there will be no repeated X coordinates.
      var i    = 0
      var size = tpls.length
      while(i < size - 1) {
        val t1 = tpls(i)
        val t2 = tpls(i + 1)
        if (t1._1 == t2._1) {
          if (t1._2 == t2._2) {
            // these two tuples represent a pointy spike so we need to assign
            // to a single pixel and remove them by shifting  everything down
            // two places.
            val k = (row + yoffset) * cols + t1._1 + xoffset
            data(k) = value
            var j = i + 2
            while (j < tpls.length) {
              tpls(j - 2) = tpls(j)
              j += 1
            }
            size -= 2
          } else {
            // these two tuples represent a hinge. we need to remove one of the
            // them and shift everything down one place.
            var j = i + 2
            while (j < tpls.length) {
              tpls(j - 1) = tpls(j)
              j += 1
            }
            size -= 1
          }
        } else {
          // these two tuples seem normal (either they are a bounding edge,
          // or a hinge, or unrelated).
          i += 1
        }
      }

      // now we can just loop over our bounds, drawing the pixels as
      // appropriate. whew!
      i = 0
      while (i < size - 1) {
        val x1 = tpls(i)._1
        val x2 = tpls(i + 1)._1
        var col = x1
        while (col <= x2) {
          val k = (row + yoffset) * cols + col + xoffset
          data(k) = value
          col += 1
        }
        i += 2
      }

      row += 1
    }
  }
}
