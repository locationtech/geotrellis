package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate

object DelaunayStitcher {
  def advance(e0: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Int = {
    var e = het.getNext(e0)
    while (!Predicates.isCorner(e))
      e = het.getNext(e)
    e
  }

  def reverse(e0: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Int = {
    var e = het.getPrev(e0)
    while (!Predicates.isCorner(e))
      e = het.getPrev(e)
    e
  }

  def advanceIfNotCorner(e0: Int)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Int = {
    var e = e0
    while (!Predicates.isCorner(e))
      e = het.getNext(e)
    e
  }

  /**
   * Finds, inserts, and returns the unique half edge on the boundary of the
   * convex hull of the two convex boundary loops given by left and right; the
   * returned edge points from right to left.  Left and right must be correctly
   * initialized to be corners of their respective bounding loops (according to
   * Predicates.isCorner), and isLeftLinear/isRightLinear must be set to true if
   * the respective meshes have only boundary edges.
   *
   * To be clear, this algorithm is impartial to absolute position despite the
   * inputs being named left and right.  It does, however, require that the
   * bounding loops represented by left and right be convex and mutually
   * non-intersecting.
   */
  def insertBase(left0: Int, isLeftLinear: Boolean, right0: Int, isRightLinear: Boolean)(implicit trans: Int => Coordinate, het: HalfEdgeTable): Int = {
    import het._
    import Predicates._

    var left = left0
    var right = right0

    if (isLeftLinear && isRightLinear) {
      // In the linear case, in the event of a linear result, we want to make 
      // sure base will be set to join the two segments at the closest points
      val lnext = advance(left)
      val rnext = advance(right)

      val l0 = trans(getSrc(left))
      val l1 = trans(getSrc(left))
      val r0 = trans(getSrc(right))
      val r1 = trans(getSrc(right))

      val d00 = l0.distance(r0)
      val d01 = l0.distance(r1)
      val d10 = l1.distance(r0)
      val d11 = l1.distance(r1)

      List(d00, d10, d01, d11).min match {
        case d if d == d01 => right = rnext
        case d if d == d10 => left = lnext
        case d if d == d11 => right = rnext
                              left = lnext
        case _ => ()
      }
    }

    // compute the lower common tangent of left and right
    var continue = true
    var base = createHalfEdges(getSrc(right), getSrc(left))

    while(continue) {
      //println("Walking the base")
      if(isLeftOf(base, getDest(left))) {
        // left points to a vertex that is to the left of
        // base, so move base to left.next
        left = advance(left)
        setDest(base, getSrc(left))
      } else if(!isRightLinear && !isRightOf(base, getDest(right))) {
        // right points to a point that is left of base,
        // so keep walking right
        right = advance(right)
        setSrc(base, getSrc(right))
      } else if(!isLeftLinear && !isRightOf(base, getSrc(getPrev(left)))) {
        // Left's previous source is left of base,
        // so this base would break convexity. Move
        // back to previous left.
        left = reverse(left)
        setDest(base, getSrc(left))
      } else if(isLeftOf(base, getSrc(getPrev(right)))) {
        // Right's previous source is left ofbase,
        // so this base would break convexity. Move
        // back to previous right.
        right = reverse(right)
        setSrc(base, getSrc(right))
      } else {
        continue = false
      }
    }

    setNext(base, left)
    setNext(getFlip(base), right)
    setNext(getPrev(left), getFlip(base))
    setNext(getPrev(right), base)

    base
  }

  /**
   * Stiches two non-intersecting Delaunay triangulations, given as half edges
   * on the outside boundary of each constituent triangulation.  
   */
  def merge(left: Int, isLeftLinear: Boolean, right: Int, isRightLinear: Boolean, triangles: TriangleMap)(implicit trans: Int => Coordinate, nav: HalfEdgeTable): (Int, Boolean) = {
    import nav._
    import Predicates._

    var base = insertBase(left, isLeftLinear, right, isRightLinear)(trans, nav)

    // If linear joins to linear, check that the current state
    // isn't already done (linear result)
    if(isLeftLinear && isRightLinear) {
      val b0 = getSrc(base)
      val b1 = getDest(base)
      val l = getDest(getNext(base))
      val r = getDest(getNext(getFlip(base)))
      if (isCollinear(b0, b1, l) && isCollinear(b0, b1, r)) {
        return (advance(getFlip(base))(trans, nav), true)
      }
    }

    var continue = true
    while(continue) {
      println("zippering")
      var lcand = rotCCWSrc(getFlip(base))
      var rcand = rotCWSrc(base)

      // Find left side candidate edge for extending the fill triangulation
      if(isCCW(getDest(lcand), getDest(base), getSrc(base))) {
        while(
          inCircle(
            getDest(base),
            getSrc(base),
            getDest(lcand),
            getDest(rotCCWSrc(lcand))
          )
        ) {
          val e = rotCCWSrc(lcand)
          triangles -= lcand
          setNext(rotCCWDest(lcand), getNext(lcand))
          setNext(getPrev(lcand), getNext(getFlip(lcand)))
          lcand = e
        }
      }

      // Find right side candidate edge for extending the fill triangulation
      if(isCCW(getDest(rcand), getDest(base), getSrc(base))) {
        while(
          inCircle(
            getDest(base),
            getSrc(base),
            getDest(rcand),
            getDest(rotCWSrc(rcand))
          )
        ) {
          val e = rotCWSrc(rcand)
          triangles -= getFlip(rcand)
          setNext(getFlip(base), rotCWSrc(rcand))
          setNext(rotCCWDest(rcand), getNext(rcand))
          rcand = e
        }
      }

      if(
        !isCCW(getDest(lcand), getDest(base), getSrc(base)) &&
        !isCCW(getDest(rcand), getDest(base), getSrc(base))
      ) {
        // no further Delaunay triangles to add
        continue = false
      } else {
        if (!isCCW(getDest(lcand), getDest(base), getSrc(base)) ||
            (
              isCCW(getDest(rcand), getDest(base), getSrc(base)) &&
              inCircle(getDest(lcand), getSrc(lcand), getSrc(rcand), getDest(rcand))
            )
          ) {
          // form new triangle from rcand and base
          val e = createHalfEdges(getDest(rcand), getDest(base))
          setNext(getFlip(e), getNext(rcand))
          setNext(e, getFlip(base))
          setNext(rcand, e)
          setNext(getFlip(lcand), getFlip(e))
          base = e
        } else {
          // form new triangle from lcand and base
          val e = createHalfEdges(getSrc(base), getDest(lcand))
          setNext(rotCCWDest(lcand), getFlip(e))
          setNext(e, getFlip(lcand))
          setNext(getFlip(e), rcand)
          setNext(getFlip(base), e)
          base = e
        }

        triangles += base
      }
    }


    (getNext(getFlip(base)), false)
  }

}
