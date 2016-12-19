package geotrellis.vector.mesh

class HalfEdgeRefNavigation[V, F] extends HalfEdgeNavigation[HalfEdgeRef[V, F], V, F] {

  type HE = HalfEdgeRef[V, F]

  def getDest(edge: HE): V = edge.vert
  def setDest(edge: HE, dest: V): Unit = edge.vert = dest

  def getNext(edge: HE): HE = edge.next
  def setNext(edge: HE, next: HE): Unit = edge.next = next

  def getFlip(edge: HE): HE = edge.flip
  def setFlip(edge: HE, flip: HE): Unit = edge.flip = flip

  def getFaceInfo(edge: HE): Option[F] = edge.face
  def setFaceInfo(edge: HE, info: Option[F]): Unit = edge.face = info

  def createHalfEdge(v: V): HE = new HalfEdgeRef(v, null, null, None)
  override def createHalfEdge(v: V, flip: HE, next: HE): HE = new HalfEdgeRef(v, flip, next, None)
}

object HalfEdgeRefNavigation {
  def apply[V, F]() = new HalfEdgeRefNavigation
}
