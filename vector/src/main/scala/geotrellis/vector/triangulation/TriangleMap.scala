package geotrellis.vector.triangulation

class TriangleMap(halfEdgeTable: HalfEdgeTable) {
  import halfEdgeTable._
  import  TriangleMap.regularizeIndex

  type TriIdx = TriangleMap.TriIdx

  private val triangles = collection.mutable.Map.empty[TriIdx, Int]

  def +=(i1: Int, i2: Int, i3: Int, edge: Int): Unit = {
    // if (isTriangle(i1,i2,i3))
    //   println(s"Attempting to re-add triangle ${(i1,i2,i3)}")
    assert(edge == getNext(getNext(getNext(edge))))
    assert(Set(i1, i2, i3) == Set(getSrc(edge), getDest(edge), getDest(getNext(edge))))
    triangles += regularizeIndex(i1, i2, i3) -> edge
    registerFace(edge)
  }

  def +=(keyValue: (TriIdx, Int)): Unit = {
    val ((a, b, c), edge) = keyValue
    //println(s"Adding triangle ($a, $b, $c)")
    // if (isTriangle(a,b,c))
    //   println(s"Attempting to re-add triangle ${(a,b,c)}")
    assert(edge == getNext(getNext(getNext(edge))))
    assert(Set(a, b, c) == Set(getSrc(edge), getDest(edge), getDest(getNext(edge))))
    triangles += TriangleMap.regularizeIndex(a, b, c) -> edge
    registerFace(edge)
  }

  def +=(edge: Int): Unit = {
    assert(edge == getNext(getNext(getNext(edge))))
    triangles += {(
      regularizeIndex(
        getDest(edge),
        getDest(getNext(edge)),
        getDest(getNext(getNext(edge)))
      ),
      edge
    )}
    registerFace(edge)
  }

  def -=(idx: TriIdx): Unit = {
    //println(s"Removing triangle $idx")
    triangles -= regularizeIndex(idx)
    removeIncidentEdge(idx._1)
    removeIncidentEdge(idx._2)
    removeIncidentEdge(idx._3)
  }

  def -=(edge: Int): Unit = {
    triangles -=
      regularizeIndex(getDest(edge), getDest(getNext(edge)), getDest(getNext(getNext(edge))))
    removeIncidentEdge(getDest(edge))
    removeIncidentEdge(getDest(getNext(edge)))
    removeIncidentEdge(getDest(getNext(getNext(edge))))
  }

  def apply(i1: Int, i2: Int, i3: Int): Int = triangles((i1, i2, i3))

  def apply(idx: TriIdx): Int = triangles(idx)

  def isTriangle(i1: Int, i2: Int, i3: Int): Boolean = triangles.contains(TriangleMap.regularizeIndex(i1, i2, i3))

  def isTriangle(idx: (Int, Int, Int)): Boolean = triangles.contains(TriangleMap.regularizeIndex(idx))

  def get(idx: TriIdx): Option[Int] = triangles.get(TriangleMap.regularizeIndex(idx))

  def get(i1: Int, i2: Int, i3: Int): Option[Int] = triangles.get(TriangleMap.regularizeIndex(i1, i2, i3))

  def getTriangles() = triangles.toMap

  def triangleVertices =
    triangles.keys

  def triangleEdges =
    triangles.values

}

object TriangleMap {
  type TriIdx = (Int, Int, Int)

  def regularizeIndex(a: Int, b: Int, c: Int): TriIdx = {
    if (a < b && a < c) (a, b, c)
    else if (b < a && b < c) (b, c, a)
    else (c, a, b)
  }

  def regularizeIndex(idx: TriIdx): TriIdx = {
    val (a, b, c) = idx
    if (a < b && a < c) idx
    else if (b < a && b < c) (b, c, a)
    else (c, a, b)
  }
}
