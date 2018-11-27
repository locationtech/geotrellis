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

package geotrellis.vector.mesh

import org.locationtech.jts.geom.Coordinate

/** Mutable, non-thread safe class to keep track of half edges
  * during Delaunay triangulation.
  *
  * Half edge meshes are representations of piecewise linear, orientable,
  * manifold surfaces, with or without boundary (no Mobius strips, edges are
  * shared by at most two facets, every vertex has a single continuous fan of
  * facets emanating from it).  Every facet in the mesh is defined by a loop of
  * half edges that are in a linked list, and every facet is joined to its
  * neighboring facets by linking complementary half edges.  In short, a half
  * edge has three pieces of information: (1) a vertex reference (dest), (2) a
  * complementary HalfEdge (flip), and (3) a pointer to the next edge on the
  * boundary of a facet.  These are sufficient to allow complete navigation of a
  * mesh.
  *
  * For convenience, derived navigation operations are provided.  Consider the
  * following diagram:
  *
  * {{{
  * v1 <---------- v2 <---------- v3
  * ^|     e1      ^|     e2      ^|
  * ||             ||             ||
  * || e3       e4 || e5       e6 ||
  * ||             ||             ||
  * |V     e7      |V     e8      |V
  * v4 ----------> v5 ----------> v6
  * }}}
  *
  * Starting from e1, `getNext` produces the sequence e1, e3, e7, e4, e1, ...
  *
  * Starting from e2, `getPrev` produces the sequence e2, e6, e8, e5, e2, ...
  *
  * `getFlip(e4) == e5` and `getFlip(e5) == e4`
  *
  * `rotCCWSrc(e4) == getFlip(e7)`
  *
  * `rotCWSrc(e4) == e8`
  *
  * `rotCCWDest(e4) == e2`
  *
  * `rotCWDest(e4) == getFlip(e1)`
  *
  * `getDest(e4) == v2`
  *
  * `getSrc(e4) == getDest(e5) == v5`
  *
  * The HalfEdgeTable contains an Array[Int] in chunks of three where each of
  * these chunks is specified as follows:
  *       [i,e1,e2] where i is the vertex of the halfedge (where the half edge 'points' to)
  *                       e1 is the halfedge index of the flip of the halfedge
  *                          (the halfedge that points to the source)
  *                       e2 is the halfedge index of the next half edge
  *                          (counter-clockwise of the triangle)
  */
class HalfEdgeTable(_size: Int) extends Serializable {
  private var size: Int = math.max(_size, 32)
  private var idx = 0
  private var edgeCount = 0

  // An object to keep an edge arriving at each vertex
  private var edgeAt = collection.mutable.Map.empty[Int, Int]

  // This array will hold the packed arrays of the halfedge table.
  private var table = Array.ofDim[Int](size * 3)

  // Since the underlying implementation uses an array we can only
  // store so many unique values. 1<<30 is the largest power of two
  // that can be allocated as an array. since each halfedge
  // takes up three slots in our array, the maximum number of those we
  // can store is Int.MaxValue / 3. so that is the maximum size we can allocate
  // for our table.
  private final val MAXSIZE: Int = Int.MaxValue / 3

  // Once our buckets get 90% full, we need to resize
  private final val FACTOR: Double = 0.9

  private var limit = (size * FACTOR).toInt

  private val freed = collection.mutable.ListBuffer.empty[Int]

  /**
   * Joins together two sections of a mesh along a common edge.
   *
   * Occasionally, it will be necessary to merge two sections of a triangulation
   * along a common edge.  This function accepts as arguments two edge
   * references which share the same endpoints.  Specifically, `e = [A -> B]`
   * and `opp = [B -> A]` where the flips of each edge are on the boundary of
   * the mesh.  The end result of this function is that `getFlip(e) = opp` and
   * `getFlip(opp) = e` and the surrounding mesh is properly connected (assuming
   * the input meshes were correctly connected).
   */
  def maxEdgeIndex() = edgeCount

  def join(e: Int, opp: Int): Unit = {
    assert(getSrc(e) == getDest(opp) && getDest(e) == getSrc(opp))

    val toKill1 = getFlip(e)
    val toKill2 = getFlip(opp)

    setNext(getPrev(getFlip(e)), getNext(getFlip(opp)))
    setNext(getPrev(getFlip(opp)), getNext(getFlip(e)))
    setFlip(e, opp)
    setFlip(opp, e)

    killEdge(toKill1)
    killEdge(toKill2)
  }

  /** Create an edge pointing to single vertex.
    * This edge will have no flip or next set.
    */
  def createHalfEdge(v: Int): Int = {
    if (freed isEmpty) {
      val e = edgeCount
      table(idx) = v
      idx += 3
      edgeCount += 1
      if (edgeCount > limit) resize()
      e
    } else {
      val e = freed.remove(0)
      table(e * 3) = v
      e
    }
  }

  /** Create an edge for a single vertex,
    * with the specified flip and next set.
    */
  def createHalfEdge(v: Int, flip: Int, next: Int): Int = {
    if (freed isEmpty) {
      val e = edgeCount
      table(idx) = v
      table(idx + 1) = flip
      table(idx + 2) = next
      idx += 3
      edgeCount += 1
      if (edgeCount > limit) { resize() }
      e
    } else {
      val e = freed.remove(0)
      table(e*3) = v
      table(e*3 + 1) = flip
      table(e*3 + 2) = next
      e
    }
  }

  /** Create two half edges that
    * both flip and point to each other
    *
    * @return the half edge point at v2
    */
  def createHalfEdges(v1: Int, v2: Int): Int = {
    val e1 = createHalfEdge(v1)
    val e2 = createHalfEdge(v2)

    setFlip(e2, e1)
    setNext(e2, e1)

    setFlip(e1, e2)
    setNext(e1, e2)

    e2
  }

  /** Create three half edges that
    * flip and point to each other
    * in a triangle.
    *
    * @return   The outer halfedge pointing at v1
    *
    * @note It's up to the caller to set these edges in counter clockwise order
    */
  def createHalfEdges(v1: Int, v2: Int, v3: Int): Int = {
    val inner1 = createHalfEdge(v1)
    val inner2 = createHalfEdge(v2)
    val inner3 = createHalfEdge(v3)

    val outer1 = createHalfEdge(v1)
    val outer2 = createHalfEdge(v2)
    val outer3 = createHalfEdge(v3)

    edgeAt += v1 -> inner1
    edgeAt += v2 -> inner2
    edgeAt += v3 -> inner3

    setNext(inner1, inner2)
    setNext(inner2, inner3)
    setNext(inner3, inner1)

    setFlip(inner1, outer3)
    setFlip(inner2, outer1)
    setFlip(inner3, outer2)

    setNext(outer1, outer3)
    setNext(outer3, outer2)
    setNext(outer2, outer1)

    setFlip(outer1, inner2)
    setFlip(outer2, inner3)
    setFlip(outer3, inner1)

    outer1
  }

  def killEdge(e: Int): Unit = {
    freed += e
    table(e * 3) = -1
    table(e * 3 + 1) = -1
    table(e * 3 + 2) = -1
  }

  /** Sets a half edge's flip half edge
    */
  def setFlip(e: Int, flip: Int): Unit =
    table(e * 3 + 1) = flip

  /** Sets a half edge's next half edge
    */
  def setNext(e: Int, next: Int): Unit =
    table(e * 3 + 2) = next

  def getFlip(e: Int): Int =
    table(e * 3 + 1)

  def getNext(e: Int): Int =
    table(e * 3 + 2)

  def getPrev(e: Int): Int = {
    var p = getNext(getFlip(e))
    while (getNext(getFlip(p)) != e) {
      p = getNext(getFlip(p))
    }
    getFlip(p)
  }

  /** Returns the vertex index for this half edge
    * (the one in which it points to)
    */
  def getDest(e: Int): Int =
    table(e * 3)

  /** Sets the vertex index for this half edge
    * (the one in which it points to)
    */
  def setDest(e: Int, v: Int): Unit =
    table(e * 3) = v

  /** Returns the source vertex index for this half edge
    * (the one in which it points from)
    */
  def getSrc(e: Int): Int = {
    val f = getFlip(e)
    table(f * 3)
  }

  /** Sets the source vertex index for this half edge
    * (the one in which it points from)
    */
  def setSrc(e: Int, v: Int): Unit =
    table(getFlip(e) * 3) = v

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge clockwise from it's source
    * along the triangulation
    */
  def rotCWSrc(e: Int): Int =
    getNext(getFlip(e))

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge counter-clockwise from it's source
    * along the triangulation
    */
  def rotCCWSrc(e: Int): Int =
    getFlip(getPrev(e))

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge clockwise from it's destination
    * along the triangulation
    */
  def rotCWDest(e: Int): Int =
    getFlip(getNext(e))

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge counter-clockwise from it's destination
    * along the triangulation
    */
  def rotCCWDest(e: Int): Int =
    getPrev(getFlip(e))

  def neighborsOf(vi: Int): Seq[Int] = {
    val nbhd = collection.mutable.ListBuffer.empty[Int]
    val e0 = getFlip(edgeIncidentTo(vi))
    var e = e0

    do {
      nbhd += getDest(e)
      e = rotCWSrc(e)
    } while (e != e0)

    nbhd.toSeq
  }

  def onBoundary(vi: Int, boundary: Int): Boolean = {
    var e = boundary
    do {
      if (getDest(e) == vi)
        return true
      e = getNext(e)
    } while (e != boundary)
    return false
  }

  def foreachInLoop(e0: Int)(f: Int => Unit): Unit = {
    var e = e0
    do {
      f(e)
      e = getNext(e)
    } while (e != e0)
  }

  def mapOverLoop[T](e0: Int)(f: Int => T): Seq[T] = {
    var e = e0
    val accum = collection.mutable.ListBuffer.empty[T]
    do {
      accum += f(e)
      e = getNext(e)
    } while (e != e0)
    accum.toSeq
  }

  def showLoop(e0: Int): Unit = {
    foreachInLoop(e0) { e => print(s"[${getSrc(e)} -> ${getDest(e)}] ") }
    println
  }

  /**
   * Provides an interactive, text based means to explore a mesh.
   *
   * @param e0 the initial edge for the navigation
   * @param trans a function that transforms vertex indices to Coordinates
   * @param addedCmds a Map from Char to (String, Int => Int) where the
   *        character is the key that will select this operation in the
   *        interface, the string is descriptive of the operation, and the
   *        Int => Int function maps the current edge to a new edge, possibly
   *        with side effects
   */
  def navigate(e0: Int, trans: Int => Coordinate, addedCmds: Map[Char, (String, Int => Int)]) = {
    val cmds = Map[Char, (String, Int => Int)](
      'k' -> ("kill (throws exception)", { _ => throw new Exception("user requested halt") }),
      'l' -> ("show loop", { e => showLoop(e); e }),
      'j' -> (("jump to vertex", { e =>
        print("Enter target vertex: ")
        val x = scala.io.StdIn.readInt
        try {
          edgeIncidentTo(x)
        } catch {
          case _: Throwable =>
            println(s"ERROR: VERTEX $x NOT FOUND")
            e
        }})),
      'n' -> ("next", getNext(_)),
      'p' -> ("prev", getPrev(_)),
      'f' -> ("flip", getFlip(_)),
      'n' -> ("next", getNext(_)),
      'w' -> ("rotCCWSrc", rotCCWSrc(_)),
      'e' -> ("rotCWSrc", rotCWSrc(_)),
      's' -> ("rotCWDest", rotCWDest(_)),
      'd' -> ("rotCCWDest", rotCCWDest(_))
    ) ++ addedCmds

    def showHelp() = {
      println("""List of commands:
                |  q: quit
                |  ?: show this menu""".stripMargin)
      cmds.foreach { case (c, (name, _)) => println(s"  ${c}: ${name}") }
    }

    var e = e0
    var continue = true

    println("Press '?<CR>' for help")

    def repl() = {
      do {
        println(s"Current edge ($e): [${getSrc(e)} -> ${getDest(e)}]\nDestination @ ${trans(getDest(e))}")

        scala.io.StdIn.readLine("> ") match {
          case "q" => continue = false
          case "?" => showHelp
          case "" => ()
          case str =>
            cmds.get(str.head) match {
              case None => println("Unrecognized command!")
              case Some((_, fn)) => e = fn(e)
            }
        }
      } while(continue)
    }

    repl
  }

  private def resize() {
    // It's important that size always be a power of 2. We grow our
    // hash table by x4 until it starts getting big, at which point we
    // only grow by x2.
    val factor = if (size < 10000) 4 else 2

    val nextSize = size * factor
    val nextTable = Array.ofDim[Int](nextSize * 3)

    // Given the underlying array implementation we can only store so
    // many unique values. given that 1<<30
    if (nextSize > MAXSIZE) sys.error("edge table has exceeded max capacity")

    var i = 0
    while (i < idx) {
      nextTable(i) = table(i)
      i += 1
    }

    size = nextSize
    table = nextTable
    limit = (size * FACTOR).toInt
  }

  /**
    * Adds the content of another HalfEdgeTable to the current table and adjusts
    * the vertex indices of the merged table (not the base object's table), if
    * desired.  The edge indices in the added table will be incremented to
    * maintain correctness.  The offset added to the edge indices will be
    * returned so that any external references may be adjusted to match.
    */
  def appendTable(that: HalfEdgeTable, reindex: Int => Int = {x => x}): Int = {
    val offset = edgeCount
    val nextCount = edgeCount + that.edgeCount
    edgeCount = nextCount
    val factor = if (nextCount < 10000) 4 else 2
    val nextSize = math.max(32, nextCount * factor)

    if (nextSize > MAXSIZE) sys.error("edge table has exceeded max capacity")

    val nextTable = Array.ofDim[Int](nextSize * 3)

    var i = 0
    while (i < idx) {
      nextTable(i) = table(i)
      i += 1
    }

    var j = 0
    while (j < that.idx) {
      if (that.table(j) != -1) {
        nextTable(i) = reindex(that.table(j))
        nextTable(i + 1) = that.table(j + 1) + offset
        nextTable(i + 2) = that.table(j + 2) + offset
      } else {
        nextTable(i) = -1
        nextTable(i + 1) = -1
        nextTable(i + 2) = -1
      }
      i += 3
      j += 3
    }

    idx += that.idx
    size = nextSize
    table = nextTable
    limit = (size * FACTOR).toInt

    freed ++= that.freed.map(_ + offset)
    edgeAt ++= that.edgeAt.map { case (v, e) => (reindex(v), e + offset) }

    offset
  }

  def edgeIncidentTo(i: Int) = edgeAt(i)
  def removeIncidentEdge(i: Int) = { edgeAt -= i }
  def setIncidentEdge(i: Int, e: Int) = {
    assert(i == getDest(e))
    edgeAt += i -> e
  }
  def registerFace(e: Int) = {
    var f = e
    do {
      setIncidentEdge(getDest(f), f)
      f = getNext(f)
    } while (f != e)
  }

  def allVertices() = edgeAt.keys

  def reindexVertices(reindex: Int => Int) = {
    var i = 0
    while (i < idx) {
      if (table(i) >= 0)
        table(i) = reindex(table(i))
      i += 3
    }
    edgeAt = edgeAt.map{ case (vi, e) => (reindex(vi), e) }
  }

}
