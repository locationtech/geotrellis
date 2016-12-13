package geotrellis.spark.pointcloud.triangulation

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
