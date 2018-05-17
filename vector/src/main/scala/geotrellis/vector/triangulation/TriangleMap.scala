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

import geotrellis.vector.mesh.HalfEdgeTable

class TriangleMap(halfEdgeTable: HalfEdgeTable) extends Serializable {
  import halfEdgeTable._
  import  TriangleMap.regularizeIndex

  type TriIdx = TriangleMap.TriIdx

  private val triangles = collection.mutable.Map.empty[TriIdx, Int]

  def +=(i1: Int, i2: Int, i3: Int, edge: Int): Unit = {
    assert(edge == getNext(getNext(getNext(edge))))
    assert(Set(i1, i2, i3) == Set(getSrc(edge), getDest(edge), getDest(getNext(edge))))
    triangles += regularizeIndex(i1, i2, i3) -> edge
    registerFace(edge)
  }

  def +=(keyValue: (TriIdx, Int)): Unit = {
    val ((a, b, c), edge) = keyValue
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
