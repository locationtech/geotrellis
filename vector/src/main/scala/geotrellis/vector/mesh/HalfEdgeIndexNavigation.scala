package geotrellis.vector.mesh

import scala.reflect.ClassTag

/** Mutable, non-thread safe class to keep track of half edges
  * during Delaunay triangulation.
  *
  * Edge table contains an Array[Int] of triples:
  *       [i,e1,e2] where i is the vertex of the halfedge (where the half edge 'points' to)
  *                       e1 is the halfedge index of the flip of the halfedge
  *                          (the halfedge that points to the source)
  *                       e2 is the halfedge index of the next half edge
  *                          (counter-clockwise of the triangle)
  */
class HalfEdgeIndexNavigation(_size: Int) extends HalfEdgeNavigation[HalfEdgeIndex, Int, Unit] {
  private var size: Int = _size
  private var idx = 0
  private var edgeCount = 0

  // private var fsize: Int = _size / 3
  // private var fidx = 0
  // private var faceCount = 0

  private final val ENTRIES = 4

  // This array will hold the packed arrays of the halfedge table.
  private var table = Array.ofDim[Int](size * ENTRIES)

  // private var ftable = Array.ofDim[F](fsize)

  // Since the underlying implementation uses an array we can only
  // store so many unique values. 1<<30 is the largest power of two
  // that can be allocated as an array. since each halfedge
  // takes up three slots in our array, the maximum number of those we
  // can store is Int.MaxValue / ENTRIES. so that is the maximum size 
  // we can allocate for our table.
  private final val MAXSIZE: Int = Int.MaxValue / ENTRIES

  // Once our buckets get 80% full, we need to resize
  private final val FACTOR: Double = 0.8

  private var limit = (size * FACTOR).toInt
  // private var flimit = (fsize * FACTOR).toInt

  /** Create an edge pointing to single vertex.
    * This edge will have no flip or next set.
    */
  def createHalfEdge(v: Int): HalfEdgeIndex = {
    val e = edgeCount
    table(idx) = v
    idx += ENTRIES
    edgeCount += 1
    if (edgeCount > limit) resize()
    new HalfEdgeIndex(e)
  }

  /** Create an edge for a single vertex,
    * with the specified flip and next set.
    */
  override def createHalfEdge(v: Int, flip: HalfEdgeIndex, next: HalfEdgeIndex): HalfEdgeIndex = {
    val e = edgeCount
    table(idx) = v
    table(idx + 1) = flip.i
    table(idx + 2) = next.i
    idx += ENTRIES
    edgeCount += 1
    if (edgeCount > limit) { resize() }
    new HalfEdgeIndex(e)
  }

  /** Create two half edges that
    * both flip and point to each other
    *
    * @return the half edge point at v2
    */
  // def createHalfEdges(v1: Int, v2: Int): HalfEdgeIndex = {
  //   val e1 = createHalfEdge(v1)
  //   val e2 = createHalfEdge(v2)

  //   setFlip(e2, e1)
  //   setNext(e2, e1)

  //   setFlip(e1, e2)
  //   setNext(e1, e2)
  //   e2
  // }

  /** Create three half edges that
    * flip and point to each other
    * in a triangle.
    *
    * @return   The outer halfedge pointing at v1
    *
    * @note It's up to the caller to set these edges in counter clockwise order
    */
  // def createHalfEdges(v1: Int, v2: Int, v3: Int): HalfEdgeIndex = {
  //   //println(s"DELAUNAY2 - HE $v1 $v2 $v3")
  //   val inner1 = createHalfEdge(v1)
  //   val inner2 = createHalfEdge(v2)
  //   val inner3 = createHalfEdge(v3)

  //   val outer1 = createHalfEdge(v1)
  //   val outer2 = createHalfEdge(v2)
  //   val outer3 = createHalfEdge(v3)

  //   setNext(inner1, inner2)
  //   setNext(inner2, inner3)
  //   setNext(inner3, inner1)

  //   setFlip(inner1, outer3)
  //   setFlip(inner2, outer1)
  //   setFlip(inner3, outer2)

  //   setNext(outer1, outer3)
  //   setNext(outer3, outer2)
  //   setNext(outer2, outer1)

  //   setFlip(outer1, inner2)
  //   setFlip(outer2, inner3)
  //   setFlip(outer3, inner1)

  //   outer1
  // }

  // def createHalfEdges(vs: Seq[Int]): HalfEdgeIndex = {
  //   val inner = vs.map(createHalfEdge(_))
  //   val outer = vs.map(createHalfEdge(_))

  //   def mkloop[T](l: Seq[HalfEdgeIndex], fst: HalfEdgeIndex): Unit = {
  //     l match {
  //       case Nil => ()
  //       case List(x) => setNext(x, fst)
  //       case x1 :: x2 :: xs => 
  //         setNext(x1, x2)
  //         mkloop(x2 :: xs, fst)
  //     }
  //   }
  //   mkloop(inner, inner.head)
  //   mkloop(outer.reverse, outer.reverse.head)

  //   inner.zip(outer.last +: outer).foreach{ case (e1, e2) =>
  //     setFlip(e1, e2)
  //     setFlip(e2, e1)
  //   }

  //   outer.head
  // }

  // // TODO: Make this match the implementation of join() in HalfEdge.scala
  // def join(e1: HalfEdgeIndex, e2: HalfEdgeIndex): Unit = {
  //   // flip.prev.next = that.flip.next
  //   setNext(getPrev(getFlip(e1)), getNext(getFlip(e2)))

  //   // that.flip.prev.next = flip.next
  //   setNext(getPrev(getFlip(e2)), getNext(getFlip(e1)))

  //   //flip = that
  //   setFlip(e1, e2)

  //   //that.flip = this
  //   setFlip(e2, e1)
  // }

  /** Sets a half edge's flip half edge
    */
  def setFlip(e: HalfEdgeIndex, flip: HalfEdgeIndex): Unit =
    table(e.i * ENTRIES + 1) = flip.i

  /** Sets a half edge's next half edge
    */
  def setNext(e: HalfEdgeIndex, next: HalfEdgeIndex): Unit =
    table(e.i * ENTRIES + 2) = next.i

  def getFlip(e: HalfEdgeIndex): HalfEdgeIndex =
    new HalfEdgeIndex(table(e.i * ENTRIES + 1))

  def getNext(e: HalfEdgeIndex): HalfEdgeIndex =
    new HalfEdgeIndex(table(e.i * ENTRIES + 2))

  // def getPrev(e: Int): Int = {
  //   var p = getNext(getFlip(e))
  //   while (getNext(getFlip(p)) != e) {
  //     p = getNext(getFlip(p))
  //   }
  //   getFlip(p)
  // }

  /** Returns the vertex index for this half edge
    * (the one in which it points to)
    */
  def getDest(e: HalfEdgeIndex): Int =
    table(e.i * ENTRIES)

  /** Sets the vertex index for this half edge
      * (the one in which it points to)
      */
  def setDest(e: HalfEdgeIndex, v: Int): Unit =
    table(e.i * ENTRIES) = v

  /** Returns the source vertex index for this half edge
    * (the one in which it points from)
    */
  override def getSrc(e: HalfEdgeIndex): Int = {
    val f = getFlip(e)
    table(f.i * ENTRIES)
  }

  /** Sets the source vertex index for this half edge
    * (the one in which it points from)
    */
  override def setSrc(e: HalfEdgeIndex, v: Int): Unit =
    table(getFlip(e).i * ENTRIES) = v

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge clockwise from it's source
    * along the triangulation
    */
  // def rotCWSrc(e: Int): Int =
  //   getNext(getFlip(e))

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge counter-clockwise from it's source
    * along the triangulation
    */
  // def rotCCWSrc(e: Int): Int =
  //   getFlip(getPrev(e))

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge clockwise from it's destination
    * along the triangulation
    */
  // def rotCWDest(e: Int): Int =
  //   getFlip(getNext(e))

  /** Returns the halfedge index of the halfedge you get
    * from rotating this halfedge counter-clockwise from it's destination
    * along the triangulation
    */
  // def rotCCWDest(e: Int): Int =
  //   getPrev(getFlip(e))

  def getFaceInfo(edge: HalfEdgeIndex): Option[Unit] = {
    val f = table(edge.i * ENTRIES + 3)
    if (f == 0)
      None
    else
      Some(())
      // Some(ftable(f))
  }

  def setFaceInfo(edge: HalfEdgeIndex, info: Option[Unit]): Unit = {
    info match {
      case None => table(edge.i * ENTRIES + 3) = 0
      case Some(attr) => {
        table(edge.i * ENTRIES + 3) = 1
        // table(edge * ENTRIES + 3) = faceCount
        // ftable(faceCount) = attr
        // faceCount += 1
        // if (faceCount > flimit) { fresize() }
      }
    }
  }

  // def getFaceInfo(edge: HalfEdge[V, F]): Option[F] = edge.face
  // def setFaceInfo(edge: HalfEdge[V, F], info: Option[F]): Unit = { edge.face = info }

  private def resize() {
    // It's important that size always be a power of 2. We grow our
    // hash table by x4 until it starts getting big, at which point we
    // only grow by x2.
    val factor = if (size < 10000) 4 else 2

    val nextSize = size * factor
    val nextTable = Array.ofDim[Int](nextSize * ENTRIES)

    println(s"RESIZE $size TO $nextSize")

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

  // private def fresize() {
  //   // It's important that size always be a power of 2. We grow our
  //   // hash table by x4 until it starts getting big, at which point we
  //   // only grow by x2.
  //   val factor = if (fsize < 10000) 4 else 2

  //   val nextSize = fsize * factor
  //   val nextTable = Array.ofDim[F](nextSize * ENTRIES)

  //   println(s"RESIZE $fsize TO $nextSize")

  //   // Given the underlying array implementation we can only store so
  //   // many unique values. given that 1<<30
  //   if (nextSize > MAXSIZE) sys.error("face table has exceeded max capacity")

  //   var i = 0
  //   while (i < fidx) {
  //     nextTable(i) = ftable(i)
  //     i += 1
  //   }

  //   fsize = nextSize
  //   ftable = nextTable
  //   flimit = (fsize * FACTOR).toInt
  // }

  // Debug methods

  def showBoundingLoop(base: HalfEdgeIndex): Unit = {
    var e = base
    var l: List[HalfEdgeIndex] = Nil

    while (!l.contains(e)) {
      l = l :+ e
      print(s"|${getSrc(e)} -> ${getDest(e)}| ")
      e = getNext(e)
    }
    println("")
  }
}
