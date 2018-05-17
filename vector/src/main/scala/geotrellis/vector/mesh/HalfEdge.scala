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

import geotrellis.vector.{Line, Point, Polygon}

class HalfEdge[V,F](val vert: V, var flip: HalfEdge[V,F], var next: HalfEdge[V,F], var face: Option[F]) extends Serializable {

  def join(that: HalfEdge[V,F]) = {
    if (vert != that.flip.vert || flip.vert != that.vert) {
      throw new IllegalArgumentException(s"Cannot join facets by edges (${flip.vert},${vert}) and (${that.vert},${that.flip.vert})")
    }

    flip.prev.next = that.flip.next
    that.flip.prev.next = flip.next
    flip = that
    that.flip = this
  }

  def neighbors(): List[V] = {
    def loop(e: HalfEdge[V,F]): List[V] = {
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

object HalfEdge {
  def foreachWithIndex[T](s: Seq[T])(f: (T,Int) => Unit) = {
    var i = 0
    while (i < s.length) {
      f(s(i), i)
      i += 1
    }
  }

  def apply[V,F](v1: V, v2: V) = {
    val e1 = new HalfEdge[V,F](v1, null, null, None)
    val e2 = new HalfEdge[V,F](v2, e1, e1, None)
    e1.flip = e2
    e1.next = e2
    e2
  }

  def apply[V,F](verts: Seq[V], f: F) = {
    val n = verts.length
    val inner = verts.map { v => new HalfEdge[V,F](v, null, null, Some(f)) }
    val outer = verts.map { v => new HalfEdge[V,F](v, null, null, None) }

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

  def showBoundingLoop[V,F](base: HalfEdge[V,F]): Unit = {
    var e = base
    var l: List[HalfEdge[V,F]] = Nil

    while (!l.contains(e)) {
      l = l :+ e
      print(s"$e ")
      e = e.next
    }
    println("")
  }

  def toPolygon[V,T](base: HalfEdge[V,T])(implicit trans: V => Point): Polygon = {
    var e = base
    var pts: List[Point] = Nil

    do {
      pts = pts :+ trans(e.vert)
      e = e.next
    } while (e != base)

    Polygon(Line(pts).closed)
  }
}
