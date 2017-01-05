package geotrellis.vector.triangulation

class TriangleMap {
  type TriIdx = TriangleMap.TriIdx

  private val triangles = collection.mutable.Map.empty[TriIdx, Int]

  def +=(i1: Int, i2: Int, i3: Int, edge: Int): Unit = {
    triangles += TriangleMap.regularizeIndex(i1, i2, i3) -> edge
  }

  def +=(keyValue: (TriIdx, Int)): Unit = {
    val ((a, b, c), edge) = keyValue
    //println(s"Adding triangle ($a, $b, $c)")
    triangles += TriangleMap.regularizeIndex(a, b, c) -> edge
  }
    
  def +=(edge: Int)(implicit het: HalfEdgeTable): Unit = {
    this += TriangleMap.regularizeIndex(het.getDest(edge), het.getDest(het.getNext(edge)), het.getDest(het.getNext(het.getNext(edge)))) -> edge
  }

  def -=(idx: TriIdx): Unit = {
    //println(s"Removing triangle $idx")
    triangles -= TriangleMap.regularizeIndex(idx)
  }

  def -=(edge: Int)(implicit het: HalfEdgeTable): Unit = {
    this -= TriangleMap.regularizeIndex(het.getDest(edge), het.getDest(het.getNext(edge)), het.getDest(het.getNext(het.getNext(edge))))
  }

  def apply(i1: Int, i2: Int, i3: Int): Int = triangles((i1, i2, i3))

  def apply(idx: TriIdx): Int = triangles(idx)

  def isTriangle(i1: Int, i2: Int, i3: Int): Boolean = triangles.contains(TriangleMap.regularizeIndex(i1, i2, i3))

  def isTriangle(idx: (Int, Int, Int)): Boolean = triangles.contains(TriangleMap.regularizeIndex(idx))

  def get(idx: TriIdx): Option[Int] = triangles.get(idx)

  def get(i1: Int, i2: Int, i3: Int): Option[Int] = triangles.get((i1, i2, i3))

  def getTriangles() = triangles.toMap
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
