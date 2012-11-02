package geotrellis.geometry

import geotrellis._
//import geotrellis.geometry.{Polygon}
import geotrellis.geometry.grid.{GridPoint, GridLine, GridPolygon}

import scala.{specialized => spec}

import scala.collection.mutable.{ArrayBuffer}
import scala.util.Sorting
import math.{abs,min,max,round}

/**
 * This object takes polygons and values and draws them into a given raster or
 * array of data.
 */
object ARasterizer {
   //trait CB[S] {
   trait CB[@spec S] {
     def apply(d: Int, z: Int, t: S):S
   }

  val DBG = -1
  //val DBG = -1

  /**
   * quantize an array of polygons based on a raster's extent, and then draw
   * them into the provided raster.
   */
  def rasterize[@spec Q](cb: CB[Q], q:Q , ext: RasterExtent, polygons:Array[Polygon]) {
    val gridPolygons = polygons.flatMap(_.translateToGrid(ext))

    //gridPolygons(0).edges.filter(_.interceptsY(DBG)).foreach(printf("  %s\n", _))

    val values = polygons.map(_.value)
    this.rasterize(cb, q, ext, gridPolygons, values)
  }

  def rasterize[@spec Q](cb: CB[Q], q: Q, ext: RasterExtent, polygons:Array[Polygon],
                fs:Array[Int]) {
    val gridPolygons = polygons.flatMap(_.translateToGrid(ext))
    this.rasterize(cb, q, ext, gridPolygons, fs)
  }

  /**
   * draw an array of polygons (with corresponding values) into raster.
   */
  def rasterize[@spec Q](cb: CB[Q], q: Q, ext: RasterExtent, polygons:Array[GridPolygon],
                fs:Array[Int]) {
    rasterize(ext.cols, ext.rows, polygons, fs, cb, q)
  }

  /* ... */
  trait Intercept {
    val x:Int
    val state:Int
    def x1 = x
    def x2 = x
    val horizontal = false
  }

  trait InterceptRange extends Intercept {
    val startx:Int
    override def x1 = startx
    override val horizontal = true
  }

  /**
   * These intercept case classes directly map to a single GridLine intersecting
   * the row we're interested in.
   */
  case class InterceptFlow(x:Int) extends Intercept { val state = 0 }
  case class InterceptStart(x:Int) extends Intercept { val state = 1 }
  case class InterceptEnd(x:Int) extends Intercept { val state = 2 }
  case class InterceptHorizontal(startx:Int, x:Int) extends InterceptRange { val state = 3 }

  /**
   * These intercepts are combinations of the previous ones. This handles
   * the situation where instead of crossing the row cleanly, the intercept
   * has an inflection point, hinge, horizontal line, etc. that makes the
   * intercept more complicated.
   */
  trait CompoundIntercept extends Intercept { val state = 9 }
  trait CompoundInterceptRange extends InterceptRange { val state = 10 }
  case class InterceptSHorizontal(startx:Int, x:Int) extends CompoundInterceptRange
  case class InterceptEHorizontal(startx:Int, x:Int) extends CompoundInterceptRange
  case class InterceptFlowHorizontal(startx:Int, x:Int) extends CompoundInterceptRange
  case class InterceptSpikeHorizontal(startx:Int, x:Int) extends CompoundInterceptRange
  case class InterceptHinge(x:Int) extends CompoundIntercept
  case class InterceptSpike(x:Int) extends CompoundIntercept

  /**
   * Given a row of interest and the lines we know will intersect it,
   * constructs an array of intercepts which fully cross our row. This
   * involves creating synthetic intercepts where necessary.
   */
  def createIntercepts(row:Int, limit:Int, group:ArrayBuffer[GridLine]) = {
    val intercepts = Array.ofDim[Intercept](limit)

    var i = 0
    //printf("row %d\n", row)
    while (i < limit) {
      val edge = group(i)
      //printf("  %s\n", edge)
      val x = edge.getx(row)

      // FIXME: save which f we are filling with
      // TODO: we could precompute which edges are spikes; would be faster.
      
      intercepts(i) = if(edge.horizontal) {
        InterceptHorizontal(edge.pmin.x, edge.pmax.x)
      } else if(edge.pmin.y == row) {
        InterceptStart(x)
      } else if (edge.pmax.y == row) {
        InterceptEnd(x)
      } else {
        InterceptFlow(x)
      }
      
      i += 1
    }

    Sorting.quickSort(intercepts)(Ordering[(Int, Int)].on[Intercept](i => (i.x1, i.x2)))

    if (row == DBG) {
      println("intercepts-1")
      intercepts.foreach(printf("  %s\n", _))
    }

    var j = 0
    while (j < limit && intercepts(j).horizontal) { j += 1}

    var k = limit
    while (k > j && intercepts(k - 1).horizontal) { k -= 1 }

    val interceptsX = Array.ofDim[Intercept](k - j)
    i = j
    while (i < k) {
      interceptsX(i - j) = intercepts(i)
      i += 1
    }

    //printf("before: %s\n", intercepts.toList)
    //printf("after:  %s\n", interceptsX.toList)

    val intercepts2 = combineIntercepts(interceptsX)
    intercepts2
  }

  /**
   * Process an array of intercepts, creating synthetic intercepts out of
   * the relevant combinations of simple intercepts.
   *
   * Examples:
   *   Start + End -> Hinge
   *   Start + Start -> Spike
   *   Start + Horizontal + Start -> HorizontalSpike
   */
  def combineIntercepts(intercepts:Array[Intercept]) = {
    val intercepts2 = ArrayBuffer[Intercept](intercepts:_*)
    var i = 0
    while(i < intercepts2.length - 1) {
      val c1 = intercepts2(i)
      val c2 = intercepts2(i + 1)
      
      val update:Option[Intercept] = (c1, c2) match {
        case (InterceptStart(x1), InterceptStart(x2)) => Some(InterceptSpikeHorizontal(x1, x2))
        case (InterceptStart(x1), InterceptEnd(x2)) => Some(InterceptFlowHorizontal(x1, x2))
        case (InterceptStart(x1), InterceptHorizontal(_, x2)) => Some(InterceptSHorizontal(x1, x2))
        case (InterceptStart(_), _) => throw new Exception("unpossible1(%d): %s".format(i, intercepts2.toList))
        
        case (InterceptEnd(x1), InterceptEnd(x2)) => Some(InterceptSpikeHorizontal(x1, x2))
        case (InterceptEnd(x1), InterceptStart(x2)) => Some(InterceptFlowHorizontal(x1, x2))
        case (InterceptEnd(x1), InterceptHorizontal(_, x2)) => Some(InterceptEHorizontal(x1, x2))
        case (InterceptEnd(_), _) => throw new Exception("unpossible2(%d): %s".format(i, intercepts2.toList))
        
        case (InterceptSHorizontal(x1, _), InterceptStart(x2)) => Some(InterceptSpikeHorizontal(x1, x2))
        case (InterceptSHorizontal(x1, _), InterceptEnd(x2)) => Some(InterceptFlowHorizontal(x1, x2))
        case (InterceptSHorizontal(x1, _), InterceptHorizontal(_, x2)) => Some(InterceptSHorizontal(x1, x2))
        case (InterceptSHorizontal(_, _), _) => throw new Exception("unpossible3()")
        
        case (InterceptEHorizontal(x1, _), InterceptEnd(x2)) => Some(InterceptSpikeHorizontal(x1, x2))
        case (InterceptEHorizontal(x1, _), InterceptStart(x2)) => Some(InterceptFlowHorizontal(x1, x2))
        case (InterceptEHorizontal(x1, _), InterceptHorizontal(_, x2)) => Some(InterceptEHorizontal(x1, x2))
        case (InterceptEHorizontal(_, _), _) => {
          throw new Exception("unpossible4") 
        }
        
        //case (InterceptHorizontal(_, _), _) => throw new Exception("unpossible5(%d): %s %s".format(i, intercepts.toList, intercepts2.toList))
        case (InterceptHorizontal(_, _), _) => Some(c2)

        //case (_, InterceptHorizontal(_, _)) => throw new Exception("unpossible6(%d): %s".format(i, intercepts.toList, intercepts2.toList))
        case (_, InterceptHorizontal(_, _)) => Some(c1)
        
        case _ => None
      }

      update match {
        case None => i += 1
        case Some(c) => {
          intercepts2(i) = c
          intercepts2.remove(i + 1)
        }
      }
    }

    // FIXME... we need something like this... but better
    // while(intercepts2.length > 1 && intercepts2(1).x2 < 0) {
    //   intercepts2.remove(0)
    //   intercepts2.remove(0)
    // }

    intercepts2
  }


  /**
   * Regions represent areas of the raster where we want to draw our
   * polygon. They contain two X coordinates and the callback to use.
   */
  case class Region(x1:Int, x2:Int, f:Int)

  /**
   * Given a row, a function, and an array of intercepts, create
   * regions wherever the polygon is visible on this row.
   */
  def createRegions(row:Int, f:Int, intercepts2:ArrayBuffer[Intercept]) = {
    val rtpls = ArrayBuffer.empty[Region]
    def lastregion() = if (rtpls.length > 0) Some(rtpls.last) else None
    var lastx:Option[Int] = None
    
    if (row == DBG) printf("start: last=%s x=%s\n", lastregion(), lastx)
    //println(intercepts2.toList)

    var i = 0
    while (i < intercepts2.length) {
      intercepts2(i) match {
        case InterceptStart(_) => None //throw new Exception("unpossibleA(%d): %s".format(i, intercepts2))
        case InterceptEnd(_) => None //throw new Exception("unpossibleB(%d): %s".format(i, intercepts2))
        case InterceptHorizontal(_, _) => None //throw new Exception("unpossibleC(%d): %s".format(i, intercepts2))

        case InterceptSHorizontal(_, _) => None //throw new Exception("unpossibleD(%d): %s".format(i, intercepts2))
        case InterceptEHorizontal(_, _) => None //throw new Exception("unpossibleE(%d): %s".format(i, intercepts2))

        case InterceptFlow(x) => lastx match {
          case None => lastx = Some(x)
          case Some(x0) => {
            rtpls.append(Region(x0, x, f))
            lastx = None
          }
        }
        
        case InterceptSpike(x) => lastx match {
          case None => rtpls.append(Region(x, x, f))
          case Some(x0) => {}
        }
        
        case InterceptHinge(x) => lastx match {
          case None => lastx = Some(x)
          case Some(x0) => {
            rtpls.append(Region(x0, x, f))
            lastx = None
          }
        }
        
        case InterceptSpikeHorizontal(x1, x2) => lastx match {
          case None => rtpls.append(Region(x1, x2, f))
          case Some(x) => {
            rtpls.append(Region(x, x1, f))
            lastx = Some(x2)
          }
        }
        
        case InterceptFlowHorizontal(x1, x2) => lastx match {
          case None => {
            rtpls.append(Region(x1, x2, f))
            lastx = Some(x2)
          }
          case Some(x) => {
            rtpls.append(Region(x, x2, f))
            lastx = None
          }
        }
        
        case s:Any => throw new Exception("could not handle %s".format(s))
        
      }
      if (row == DBG) printf("  after %s: last=%s x=%s\n", intercepts2(i), lastregion(), lastx)
      i += 1
    }
    
    // lastx match {
    //   case Some(x) => printf("ROW %d HAD LEFTOVER: %s\n", row, x)
    //   case _ => {}
    // }

    //println(rtpls.toList)
    while (rtpls.length > 0 && rtpls(0).x2 < 0) { rtpls.remove(0) }
    rtpls.toArray
  }

  /**
   * draw an array of polygons (with corresponding values) into an integer
   * array with the dimensions: cols x rows.
   */
  def rasterize[Q](cols:Int, rows:Int, polygons:Array[GridPolygon],
                fs:Array[Int], cb: CB[Q], q: Q) {
    //printf("rasterize called with cols=%d and rows=%d\n", cols, rows)
    var qq = q
    val lastrow = rows - 1
    val lastcol = cols - 1

    // we want to create an array of layers; each layer has a polygon (which we
    // are representing as a set of edges) and a corresponding fill value.
    val g = polygons.map {
      p => {
        // we sort the line segments according to which is uppermost (and
        // leftmost). this is important because our rasterization starts at the
        // top and works down, so we want the relevant lines first.

        // we filter out horizontal lines because they are implied by our
        // algorithm... the edges points will be reported by the segments
        // connected to the horizontal line.
        //val lines = p.edges.filter(!_.horizontal).toArray
        val lines = p.edges.toArray

        Sorting.quickSort(lines)
        ArrayBuffer[GridLine](lines:_*)
      }
    }

    // groups(i) are the edges for a given polygon, and gfs(i) is the value
    // which that polygon will be filled to. glimits(i) is the limit of the
    // currently relevant edges (one beyond the last index).
    val groups  = ArrayBuffer[ArrayBuffer[GridLine]](g:_*)
    val gfs = ArrayBuffer[Int](fs:_*)
    var glimits = ArrayBuffer.fill[Int](groups.length)(0)

    // loop over each row (Y coordinate) drawing horizontal lines where
    // appropriate for particular polygons. earlier polygons in groups will
    // cover up later ones.
    var row = 0

    // println("sorted edges:")
    // for (edge <- groups(0)) {
    //   printf("  %d/%d %s\n", edge.pmin.y, edge.pmax.y, edge)
    // }

    while (row < rows) {
      //printf("row %d\n", row)

      //assert(groups.length == gfs.length)
      //assert(groups.length == glimits.length)

      // for each of our groups, figure out which line segments are relevant
      // now and adjust limits as needed.
      var gi = 0
      while (gi < groups.length) {
        val group = groups(gi)

        // for each of the edges, there are some options:
        //
        // 1. we haven't reached this edge (or any subsequent edge) yet, so
        //    we're done
        // 2. we have just arrived at the edge, in which case we need to
        //    increment the limit and check the next edge (if applicable)
        // 3. we are still handling this edge, so keep checking.
        // 4. we are done with this edge, so we should remove it from the group
        //    (adjusting the limit) and continue checking.
        var ei = 0
        while (ei < group.length && ei <= glimits(gi)) {
          // case #1 is handled by the loop condition
          val edge = group(ei)

          if (edge.pmax.y < row) {
            // this handles case #4
            group.remove(ei)
            if (ei < glimits(gi)) {
              glimits(gi) = max(glimits(gi) - 1, 0)
            }
            if (row == DBG) printf("%d/%d: %d/%d removing %s (case #4)\n", ei, glimits(gi), edge.pmin.y, edge.pmax.y, edge)
          } else {
            // this handles cases #2 and #3
            if (ei == glimits(gi) && edge.pmin.y <= row) {
              // this handles case #2 specifically
              glimits(gi) += 1
              if (row == DBG) printf("%d/%d: increasing limit: %s (case #2)\n", ei, glimits(gi), edge)
            } else {
              if (row == DBG) printf("%d/%d: shrug? %s (case #3)\n", ei, glimits(gi), edge)
            }

            ei += 1
          }
        }

        // if we have removed all the edges from a group, we should remove it
        // from groups since all its edges are done. then keep checking the
        // other groups
        if (group.length > 0) {
          gi += 1
        } else {
          groups.remove(gi)
          gfs.remove(gi)
          glimits.remove(gi)
        }
      }

      // if (row == DBG) {
      //   printf("row = %d\n", row)
      //   for (k <- 0 until glimits(0)) {
      //     printf("  groups(0)(%d) = %s\n", k, groups(0)(k))
      //   }
      // }

      // if we have no groups left then we're done
      if (groups.length == 0) return

      // at this point we know exactly which line segments for each layer are
      // relevant (via groups and glimits). now we need to figure out for each
      // line segment what X coordinate range it wants to fill. remember that
      // earlier layers trump later ones.

      // first we create Intercepts for each line we are considering; each one
      // represents a line. These lines can flow through this row, they can
      // start on this row or they can end on this row. we need to use the
      // start and end information to find "spikes" and "hinges" which need
      // special treatment.
      val gregions = Array.ofDim[Array[Region]](groups.length)

      gi = 0
      while (gi < groups.length) {
        // get the callback we are going to use to color this line
        val f = gfs(gi)

        // figure out what points our polygon intercepts this line
        val intercepts = createIntercepts(row, max(glimits(gi), 0), groups(gi))

        // create the regions we are going to color in on this line
        // based on the intercepts we found
        gregions(gi) = createRegions(row, f, intercepts)
        gi += 1
      }

      if (row == DBG) printf("gregions: %s\n", gregions.map(_.toList).toList)

      // now we need to integrate each "layer" into a single list of ranges.
      // for each layer other than the first, this may mean splitting a range
      // (because a previous range will overwrite part of it).
      val xtpls = ArrayBuffer[Region](gregions(0):_*)
      for (gi <- 1 until gregions.length) {
        val tpls = gregions(gi)
        if (tpls.length > 0) regionCombine(xtpls, tpls)
      }

      if (row == DBG) printf("xregions: %s\n", xtpls.toList)
      
      // now we can just loop over our bounds, drawing the pixels as
      // appropriate. whew!
      xtpls.foreach {
        t => {
          val Region(x1, x2, f) = t
          var col = max(x1, 0)
          if (row == DBG) printf("handling: %s (starting at %d, until %d/%d)\n", t, col, x2, lastcol)
          while (col <= x2 && col <= lastcol) {
            val k = (lastrow - row) * cols + col
            qq = cb(k, f, qq)
            //if (row == DBG) printf("  writing %d into %d (%d)\n", data(k), col, k)
            col += 1
          }
        }
      }

      row += 1
    }

    qq
  }

  /**
   * add a new layer to our final set of ranges (splitting ranges where they
   * intersect with existing ranges).
   */
  def regionCombine(ranges:ArrayBuffer[Region], layer:Array[Region]) {
    var i   = 0
    var k   = 0
    val len = layer.length
    while (k < ranges.length && i < len) {
      val newRange = layer(i)
      val oldRange = ranges(k)

      if (newRange.x2 < oldRange.x1) {
        // the new layer to be added is before the current old range, so we can
        // just prepend it in front of old range
        ranges.insert(k, newRange)

        // we have handled this new range, so move to the next one.
        i += 1

      } else if (oldRange.x2 < newRange.x1) {
        // the current old range is entirely before the new range, so we need
        // shift forward to the next old range before we can handle the newer.
        k += 1

      } else {
        // the new and old range intersect in some way. so let's split the
        // new range across the old one and see what happens
        val (beforeOldRange, afterOldRange) = tokenSplit(oldRange, newRange)
        if (beforeOldRange.isDefined) {
          // part of the new range goes before the old range
          ranges.insert(k, beforeOldRange.get)
        }
        if (afterOldRange.isDefined) {
          // part of the new range goes after the old range. in this case we
          // need to update our current old range because we know future new
          // ranges will also be past the current old range.
          ranges.insert(k + 1, afterOldRange.get)
          k += 1
        }

        // we have handled this new range, so move to the next one.
        i += 1
      }
    }

    // if we have reached the end of the current ranges and have more ranges
    // to add, drop them all at the end.
    while (i < len) {
      ranges.append(layer(i))
      i += 1
    }
  }


  /** in the case where an old and new range intersect, we need to modify the
   * new range (either chopping off part of it or splitting it) to hide the
   * part which is "covered up" by the old range.
   */
  def tokenSplit(older:Region, added:Region) = {
    // these are the x, y and function for the older region
    val Region(ox1, ox2, of) = older

    // these are the x, y and function for the newer region
    val Region(ax1, ax2, af) = added

    // check if the new range starts before the old one.
    val before = if (ax1 < ox1) Some(Region(ax1, ox1 - 1, af)) else None

    // check if the new range ends after the old one.
    val after = if (ax2 > ox2) Some(Region(ox2 + 1, ax2, af)) else None

    // return the optional subtokens.
    (before, after)
  }
}
