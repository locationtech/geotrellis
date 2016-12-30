/*
 * Copyright 2016 Azavea
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

package geotrellis.pointcloud.spark.triangulation

import scala.collection.mutable

class TriangleMap(halfEdgeTable: HalfEdgeTable) {
  import halfEdgeTable._

  private val _triangles =
    mutable.Map.empty[(Int, Int, Int), Int]

  def triangleVertices =
    _triangles.keys

  def triangles =
    _triangles.values

  def insertTriangle(v1: Int, v2: Int, v3: Int, e: Int): Unit =
    if(v1 < v2 && v1 < v3) { _triangles += (((v1, v2, v3), e)) }
    else if(v2 < v1 && v2 < v3) { _triangles += (((v2, v3, v1), e)) }
    else { _triangles += (((v3, v1, v2), e)) }

  def insertTriangle(e: Int): Unit =
    insertTriangle(getVert(e), getVert(getNext(e)), getVert(getNext(getNext(e))), e)

  def deleteTriangle(v1: Int, v2: Int, v3: Int): Unit =
    if(v1 < v2 && v1 < v3) { _triangles -= ((v1, v2, v3)) }
    else if(v2 < v1 && v2 < v3) { _triangles -= ((v2, v3, v1)) }
    else { _triangles -= ((v3, v1, v2)) }

  def deleteTriangle(e: Int): Unit =
    deleteTriangle(getVert(e), getVert(getNext(e)), getVert(getNext(getNext(e))))
}

object TriangleMap {
  def regularizeTriangleIndex (v1: Int, v2: Int, v3: Int): (Int, Int, Int) = {
    if(v1 < v2 && v1 < v3) { (v1, v2, v3) }
    else if(v2 < v1 && v2 < v3) { (v2, v3, v1) }
    else { (v3, v1, v2) }
  }
}
