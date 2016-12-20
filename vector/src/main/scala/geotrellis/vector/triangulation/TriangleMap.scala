package geotrellis.vector.triangulation

class TriangleMap {
  type TriIdx = (Int, Int, Int)
  val triangles = collection.mutable.Map.empty[TriIdx, Int]

  def regularizeIndex(a: Int, b: Int, c: Int): TriIdx = {
    if (a < b && a < c) (a, b, c)
    else if (b < a && b < c) (b, c, a)
    else (c, a, b)
  }

  def regularizeIndex(idx: (Int, Int, Int)): TriIdx = {
    val (a, b, c) = idx
    if (a < b && a < c) idx
    else if (b < a && b < c) (b, c, a)
    else (c, a, b)
  }

  def +=(i1: Int, i2: Int, i3: Int, edge: Int): Unit = {
    triangles += regularizeIndex(i1, i2, i3) -> edge
  }

  def +=(keyValue: (TriIdx, Int)): Unit = {
    val ((a, b, c), edge) = keyValue
    triangles += regularizeIndex(a, b, c) -> edge
  }
    
  def +=(edge: Int)(implicit het: HalfEdgeTable): Unit = {
    this += regularizeIndex(het.getDest(edge), het.getDest(het.getNext(edge)), het.getDest(het.getNext(het.getNext(edge)))) -> edge
  }

  def -=(idx: TriIdx): Unit = triangles -= regularizeIndex(idx)

  def -=(edge: Int)(implicit het: HalfEdgeTable): Unit = {
    triangles -= regularizeIndex(het.getDest(edge), het.getDest(het.getNext(edge)), het.getDest(het.getNext(het.getNext(edge))))
  }

  def getTriangles() = triangles.toMap
}
