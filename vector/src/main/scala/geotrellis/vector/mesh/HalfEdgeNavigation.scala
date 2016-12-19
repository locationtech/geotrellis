package geotrellis.vector.mesh

trait HalfEdgeNavigation[HE, V, F] {
  def getDest(edge: HE): V
  def setDest(edge: HE, dest: V): Unit

  def getSrc(edge: HE): V = getDest(getFlip(edge))
  def setSrc(edge: HE, src: V): Unit = setDest(getFlip(edge), src)

  def getPrev(edge: HE): HE = {
    var e = getNext(getFlip(edge))
    while (getNext(getFlip(e)) != edge) {
      e = getNext(getFlip(e))
    }
    getFlip(e)
  }
 
  def getNext(edge: HE): HE
  def setNext(edge: HE, next: HE): Unit

  def getFlip(edge: HE): HE
  def setFlip(edge: HE, flip: HE): Unit

  def rotCWSrc(edge: HE): HE = getNext(getFlip(edge))
  def rotCCWSrc(edge: HE): HE = getFlip(getPrev(edge))
  def rotCWDest(edge: HE): HE = getFlip(getNext(edge))
  def rotCCWDest(edge: HE): HE = getPrev(getFlip(edge))

  def getFaceInfo(edge: HE): Option[F]
  def setFaceInfo(edge: HE, info: Option[F]): Unit

  def createHalfEdge(v: V): HE
  def createHalfEdge(v: V, flip: HE, next: HE): HE = {
    val e = createHalfEdge(v)
    setFlip(e, flip)
    setNext(e, next)
    e
  }

  def createHalfEdges(v1: V, v2: V): HE = {
    val to = createHalfEdge(v2)
    val from = createHalfEdge(v1)

    setNext(to, from)
    setNext(from, to)

    setFlip(from, to)
    setFlip(to, from)

    to
  }

  def createHalfEdges(v1:V, v2: V, v3: V): HE = {
    val inner1 = createHalfEdge(v1)
    val inner2 = createHalfEdge(v2)
    val inner3 = createHalfEdge(v3)

    val outer1 = createHalfEdge(v1)
    val outer2 = createHalfEdge(v2)
    val outer3 = createHalfEdge(v3)

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

  def createHalfEdges(vs: Seq[V]): HE = {
    val inner = vs.map(createHalfEdge(_))
    val outer = vs.map(createHalfEdge(_))

    def mkloop[T](l: Seq[HE], fst: HE): Unit = {
      l match {
        case Nil => ()
        case List(x) => setNext(x, fst)
        case x1 :: x2 :: xs => 
          setNext(x1, x2)
          mkloop(x2 :: xs, fst)
      }
    }
    mkloop(inner, inner.head)
    mkloop(outer.reverse, outer.reverse.head)

    inner.zip(outer.last +: outer).foreach{ case (e1, e2) =>
      setFlip(e1, e2)
      setFlip(e2, e1)
    }

    outer.head
  }


  def join(e1: HE, e2: HE): Unit = {
    setNext(getPrev(getFlip(e1)), getNext(getFlip(e2)))
    setNext(getPrev(getFlip(e2)), getNext(getFlip(e1)))
    setFlip(e1, e2)
    setFlip(e2, e1)
  }
}
