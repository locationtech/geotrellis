/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector.triangulation

import org.locationtech.jts.geom.Coordinate
import geotrellis.vector.{Line, MultiLine, Point}
import geotrellis.vector.RobustPredicates.{LEFTOF, ON, RIGHTOF}
import geotrellis.vector.mesh.{HalfEdgeTable, IndexedPointSet}

final class DelaunayStitcher(
  pointSet: IndexedPointSet,
  halfEdgeTable: HalfEdgeTable
) extends Serializable {
  val predicates = new TriangulationPredicates(pointSet, halfEdgeTable)
  import predicates._
  import pointSet._
  import halfEdgeTable._

  def advance(e0: Int): Int = {
    var e = getNext(e0)
    while (!isCorner(e))
      e = getNext(e)
    e
  }

  def reverse(e0: Int): Int = {
    var e = getPrev(e0)
    while (!isCorner(e))
      e = getPrev(e)
    e
  }

  def advanceIfNotCorner(e0: Int): Int = {
    var e = e0
    while (!isCorner(e))
      e = getNext(e)
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
  def insertBase(left0: Int, isLeftLinear: Boolean, right0: Int, isRightLinear: Boolean, debug: Boolean): Int = {
    if (debug) println("Finding base ...")

    var left = advanceIfNotCorner(left0)
    var right = advanceIfNotCorner(right0)

    // compute the lower common tangent of left and right
    var continue = true
    var base = createHalfEdges(getSrc(right), getSrc(left))

    // Walking the base.  Many problems can arise here.  We're looking for
    // a base edge that is on the convex hull of the combined point sets
    // of the left and right sub-triangulations, with a src (dest) vertex
    // in the right (left) sub-triangulation.  That means that no points
    // can lie to the left of the chosen base.  We have to make our
    // decisions about what to do based on the local configuration around
    // left and right, which are half edges on the exterior bounding loop
    // of each triangulation.  Induction assures us that exterior loops
    // are always wound clockwise and the loops are semi-convex (i.e.,
    // e.dest is never left of e.prev, though possibly it is on e.prev).
    // For each left and right, there are 9 local configurations that
    // matter based on whether e.dest and e.prev.src are left of/right
    // of/on the current base.  Because we always use advance and reverse
    // to navigate among corners of the bounding loops, the logic is
    // simplified a bit.  Note that right and left are handled differently
    // due to the potential for "flat spots" on the combined convex hull,
    // which are defined to be convex hull edges with more than two points
    // of the combined point set lying on the line.  The following tables
    // define our actions in the salient cases (spaces with dashes are
    // never encountered if the winding of the exterior boundary is indeed
    // clockwise):
    //
    //                                           left.dest is ___ base
    //                                       RIGHT OF     ON      LEFT OF
    //                            RIGHT OF      OK        OK      ADVANCE
    // left.prev.src is ___ base  ON          REVERSE    ????     ADVANCE
    //                            LEFT OF     REVERSE   ADVANCE   ADVANCE
    //
    //                                           right.dest is ___ base
    //                                        RIGHT OF     ON      LEFT OF
    //                             RIGHT OF      OK      ADVANCE   ADVANCE
    // right.prev.src is ___ base  ON            OK       ????     ADVANCE
    //                             LEFT OF     REVERSE   REVERSE   REVERSE
    //
    // Linear sub-triangulations pose a special case.  Since, by virtue of
    // using advance and reverse, e's source vertex must be a corner,
    // e.dest and e.prev.src should always be *the same* vertex for linear
    // triangulations.  Thus, both are right of, left of, or on the base
    // candidate.  When both are left or right of base, we need no special
    // handling, but when both are on base, we only need to advance if
    // e.dest lies inside the line segment from base.src to base.dest.
    while(continue) {
      val ldRel = relativeTo(base, getDest(left))
      if (debug) println(s"Candidate: ${getSrc(base)} -> ${getDest(base)}, left.dest: ${getDest(left)}")
      if (ldRel == LEFTOF) {
        if (debug) println("Left dest is LEFTOF base (advance left)")
        left = advance(left)
        setDest(base, getSrc(left))
      } else {
        val lpsRel = relativeTo(base, getSrc(getPrev(left)))
        if (!(lpsRel == RIGHTOF ||
              (lpsRel == ON && ldRel == ON &&
                distance(getSrc(base), getDest(base)) < distance(getSrc(base), getDest(left))))) {
          // left still needs to be moved
          if (ldRel == RIGHTOF) {
            if (debug) println(s"Left previous source is not RIGHTOF base and left dest is RIGHTOF base (reverse left)")
            left = reverse(left)
            setDest(base, getSrc(left))
          } else {
            if (debug) println(s"Left dest is ON base (advance left)")
            left = advance(left)
            setDest(base, getSrc(left))
          }
        } else {
          // left is acceptable, try to adjust right
          val rpsRel = relativeTo(base, getSrc(getPrev(right)))
          if (rpsRel == LEFTOF) {
            if (debug) println(s"Right previous source is LEFTOF base (reverse right)")
            right = reverse(right)
            setSrc(base, getSrc(right))
          } else {
            val rdRel = relativeTo(base, getDest(right))
            if (!(rdRel == RIGHTOF ||
                  (rdRel == ON && rpsRel == ON &&
                   distance(getDest(base), getSrc(base)) < distance(getDest(base), getDest(right))))) {
              if (debug) println(s"Right dest is not RIGHTOF base, or right dest and prev src are ON base")
              right = advance(right)
              setSrc(base, getSrc(right))
            } else {
              continue = false
            }
          }
        }
      }
    }

    setNext(base, left)
    setNext(getFlip(base), right)
    setNext(getPrev(left), getFlip(base))
    setNext(getPrev(right), base)

    if (debug) println(s"Found base: ${getSrc(base)} -> ${getDest(base)}")

    base
  }

  // A function to make code readable.  The `e` parameter will refer to
  // either the left or right candidate edges for extending the stitch.  If
  // the candidate is not right of the base edge, then it cannot participate
  // in a triangle with `b`, and that candidate will be considered invalid.
  @inline final def valid(e: Int, b: Int) = isCCW(getDest(e), getDest(b), getSrc(b))

  /**
   * Stiches two non-intersecting Delaunay triangulations, given as half edges
   * on the outside boundary of each constituent triangulation.
   */
  def merge(left: Int, isLeftLinear: Boolean, right: Int, isRightLinear: Boolean, triangles: TriangleMap, debug: Boolean = false): (Int, Boolean) = {
    var base = insertBase(left, isLeftLinear, right, isRightLinear, debug)

    // If linear joins to linear, check that the current state
    // isn't already done (linear result)
    if(isLeftLinear && isRightLinear) {
      val b0 = getSrc(base)
      val b1 = getDest(base)
      val l = getDest(getNext(base))
      val r = getDest(getNext(getFlip(base)))
      if (isCollinear(b0, b1, l) && isCollinear(b0, b1, r)) {
        return (advance(getFlip(base)), true)
      }
    }

    val allEs = collection.mutable.Set.empty[Line]

    var continue = true
    while(continue) {
      if (debug) {
        println(s"Base = [${getSrc(base)} -> ${getDest(base)}]")
      }

      var lcand = rotCCWSrc(getFlip(base))
      var rcand = rotCWSrc(base)

      if (debug) {
        println(s"Initial LCAND = [${getSrc(lcand)} -> ${getDest(lcand)}]")
        println(s"Initial RCAND = [${getSrc(rcand)} -> ${getDest(rcand)}]")
      }

      // Find left side candidate edge for extending the fill triangulation
      if(valid(lcand, base)) {
        while(
          inCircle(
            getDest(base),
            getSrc(base),
            getDest(lcand),
            getDest(rotCCWSrc(lcand))
          )
        ) {
          val e = rotCCWSrc(lcand)

          if (debug) {
            println(s"Deleting LCAND")
            println(s"Advancing LCAND to [${getSrc(e)} -> ${getDest(e)}]")
          }

          triangles -= lcand
          setNext(rotCCWDest(lcand), getNext(lcand))
          setNext(getPrev(lcand), getFlip(base))
          killEdge(getFlip(lcand))
          killEdge(lcand)
          
          lcand = e

          if (debug) {
            val c = circleCenter(getDest(base), getSrc(base), getDest(lcand))._2
            val r = c.distance(getCoordinate(getDest(base)))

            println(s"   [31m✘[0m Deleted LCAND [circle center = $c, radius = $r]")
          }
        }
      }



      // Find right side candidate edge for extending the fill triangulation
      if(valid(rcand, base)) {
        while(
          inCircle(
            getDest(base),
            getSrc(base),
            getDest(rcand),
            getDest(rotCWSrc(rcand))
          )
        ) {
          val e = rotCWSrc(rcand)

          if (debug) {
            println(s"Deleting RCAND")
            println(s"Advancing RCAND to [${getSrc(e)} -> ${getDest(e)}]")
          }

          triangles -= getFlip(rcand)
          setNext(getFlip(base), rotCWSrc(rcand))
          setNext(rotCCWDest(rcand), getNext(rcand))
          killEdge(getFlip(rcand))
          killEdge(rcand)

          rcand = e

          if (debug) {
            val c = circleCenter(getDest(base), getSrc(base), getDest(rcand))._2
            val r = c.distance(getCoordinate(getDest(base)))

            println(s"   [31m✘[0m Deleted RCAND [circle center = $c, radius = $r]")
          }
        }
      }

      if(
        !valid(lcand, base) &&
        !valid(rcand, base)
      ) {
        // no further Delaunay triangles to add
        continue = false
      } else {
        if (!valid(lcand, base) || (valid(rcand, base) && inCircle(getDest(lcand), getSrc(lcand), getSrc(rcand), getDest(rcand)))
          ) {
          // form new triangle from rcand and base
          val e = createHalfEdges(getDest(rcand), getDest(base))
          setNext(getFlip(e), getNext(rcand))
          setNext(e, getFlip(base))
          setNext(rcand, e)
          setNext(getFlip(lcand), getFlip(e))
          triangles += getFlip(base)

          if (debug) {
            val t = getFlip(base)
            println(s"   [32m✔[0m Seleted RCAND, creating triangle ${(getSrc(t), getDest(t), getDest(getNext(t)))}")
          }

          base = e
        } else {
          // form new triangle from lcand and base
          val e = createHalfEdges(getSrc(base), getDest(lcand))
          setNext(rotCCWDest(lcand), getFlip(e))
          setNext(e, getFlip(lcand))
          setNext(getFlip(e), rcand)
          setNext(getFlip(base), e)
          triangles += getFlip(base)

          if (debug) {
            val t = getFlip(base)
            println(s"   [32m✔[0m Seleted LCAND, creating triangle ${(getSrc(t), getDest(t), getDest(getNext(t)))}")
          }

          base = e
        }

      }
    }

    (advance(getFlip(base)), false)
  }

  private def shouldAdvance(b: Int, e: Int) = {
    relativeTo(b, getDest(e)) match {
      case LEFTOF => true
      case RIGHTOF =>
        relativeTo(b, getSrc(getPrev(e))) match {
          case LEFTOF => true
          case RIGHTOF =>  false
          case ON => true
        }
      case ON =>
        relativeTo(b, getSrc(getPrev(e))) match {
          case RIGHTOF => false
          case LEFTOF => true
          case ON => distance(getSrc(b), getDest(b)) > distance(getSrc(b), getDest(e))
        }
    }
  }

  def joinToVertex(bound: Int, isLinear: Boolean, vertex: Int, triangles: TriangleMap, debug: Boolean = false) = {
    var e = advanceIfNotCorner(bound)
    var base = createHalfEdges(vertex, getSrc(e))

    while (shouldAdvance(base, e)) {
      e = advance(e)
      setDest(base, getSrc(e))
    }
    setNext(getPrev(e), getFlip(base))
    setNext(base, e)
    if (debug) println(s"Found base [$base]: [${getSrc(base)} ⇒ ${getDest(base)}], ${(getCoordinate(getSrc(base)), getCoordinate(getDest(base)))}")

    if (isLinear && relativeTo(base, getDest(e)) == ON) {
      (base, true)
    } else {
      var cand = rotCCWSrc(getFlip(base))

      while(valid(cand, base)) {
        while(inCircle(getDest(base), getSrc(base), getDest(cand), getDest(rotCCWSrc(cand)))) {
          val hold = rotCCWSrc(cand)

          if (debug) println(s"Deleting [${getSrc(cand)} -> ${getDest(cand)}]")

          triangles -= cand
          setNext(rotCCWDest(cand), getNext(cand))
          setNext(getPrev(cand), getFlip(base))
          killEdge(getFlip(cand))
          killEdge(cand)

          cand = hold
        }

        if (debug) println(s"Found candidate [$cand]: [${getSrc(cand)} ⇒ ${getDest(cand)}], ${(getCoordinate(getSrc(cand)), getCoordinate(getDest(cand)))}")
        val added = createHalfEdges(getSrc(base), getDest(cand))
        if (debug) println(s"Adding [$added]: [${getSrc(added)} ⇒ ${getDest(added)}]")

        setNext(rotCCWDest(cand), getFlip(added))
        setNext(added, getFlip(cand))
        setNext(getFlip(added), getNext(getFlip(base)))
        setNext(getFlip(base), added)
        triangles += added
        if (debug) println(s"Added triangle ${(getSrc(added), getDest(added), getDest(getNext(added)))}")

        cand = rotCCWSrc(getFlip(added))
        base = added
      }

      val `final` = advance(getFlip(base))
      (`final`, false)
    }
  }

}
