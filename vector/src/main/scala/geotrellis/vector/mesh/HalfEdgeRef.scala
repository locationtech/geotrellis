package geotrellis.vector.mesh

import geotrellis.vector.{Line, Point, Polygon}

class HalfEdgeRef[V,F](var vert: V, var flip: HalfEdgeRef[V,F], var next: HalfEdgeRef[V,F], var face: Option[F]) 
  extends HalfEdge
{
  def getDest(edge: HalfEdgeRef[V, F]): V = edge.vert
  def setDest(edge: HalfEdgeRef[V, F], dest: V): Unit = { edge.vert = dest }

  def getNext(edge: HalfEdgeRef[V, F]) = edge.next
  def setNext(edge: HalfEdgeRef[V, F], newNext: HalfEdgeRef[V, F]) = { edge.next = newNext }

  def getFlip(edge: HalfEdgeRef[V, F]): HalfEdgeRef[V, F] = edge.flip
  def setFlip(edge: HalfEdgeRef[V, F], flip: HalfEdgeRef[V, F]): Unit = { edge.flip = flip }

  def getFaceInfo(edge: HalfEdgeRef[V, F]): Option[F] = edge.face
  def setFaceInfo(edge: HalfEdgeRef[V, F], info: Option[F]): Unit = { edge.face = info }

  def join(that: HalfEdgeRef[V,F]) = {
    if (vert != that.flip.vert || flip.vert != that.vert) {
      throw new IllegalArgumentException(s"Cannot join facets by edges (${flip.vert},${vert}) and (${that.vert},${that.flip.vert})")
    }

    next.flip.next = that.flip.next
    that.next.flip.next = flip.next
    flip = that
    that.flip = this
  }

  def neighbors(): List[V] = {
    def loop(e: HalfEdgeRef[V,F]): List[V] = {
      if (e == this) {
        List(e.flip.vert)
      } else {
        e.flip.vert :: loop(e.next.flip)
      }
    }
    loop(this.flip.next)
  }

  def prev() = {
    var e = flip.next
    while (e.flip.next != this) {
      e = e.flip.next
    }
    e.flip
  }

  def src() = flip.vert

  def rotCWSrc() = flip.next

  def rotCCWSrc() = prev.flip

  def rotCWDest() = next.flip

  def rotCCWDest() = flip.prev

  override def toString() = { s"[${src} -> ${vert}]" }
}

object HalfEdgeRef {
  def foreachWithIndex[T](s: Seq[T])(f: (T,Int) => Unit) = {
    var i = 0
    while (i < s.length) {
      f(s(i), i)
      i += 1
    }
  }

  def apply[V,F](v1: V, v2: V) = {
    val e1 = new HalfEdgeRef[V,F](v1, null, null, None)
    val e2 = new HalfEdgeRef[V,F](v2, e1, e1, None)
    e1.flip = e2
    e1.next = e2
    e2
  }

  def apply[V,F](verts: Seq[V], f: F) = {
    val n = verts.length
    val inner = verts.map { v => new HalfEdgeRef[V,F](v, null, null, Some(f)) }
    val outer = verts.map { v => new HalfEdgeRef[V,F](v, null, null, None) }

    foreachWithIndex(inner){
      (e,i) => {
        e.next = inner((i+1) % n)
        e.flip = outer((i-1+n) % n)
      }
    }
    foreachWithIndex(outer){
      (e,i) => {
        e.next = outer((i-1+n) % n)
        e.flip = inner((i+1) % n)
      }
    }

    outer(0)
  }

  def showBoundingLoop[V,F](base: HalfEdgeRef[V,F]): Unit = {
    var e = base
    var l: List[HalfEdgeRef[V,F]] = Nil

    while (!l.contains(e)) {
      l = l :+ e
      print(s"$e ")
      e = e.next
    }
    println("")
  }

  def toPolygon[V,T](base: HalfEdgeRef[V,T])(implicit trans: V => Point): Polygon = {
    var e = base
    var pts: List[Point] = Nil

    do {
      pts = pts :+ trans(e.vert)
      e = e.next
    } while (e != base)

    Polygon(Line(pts).closed)
  }
}
