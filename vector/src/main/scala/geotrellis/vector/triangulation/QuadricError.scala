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

import org.locationtech.jts.geom.Coordinate
import geotrellis.vector.mesh.HalfEdge
import org.apache.commons.math3.linear.MatrixUtils

object QuadricError {

  def facetMatrix(tris: Traversable[(Int, Int, Int)], trans: Int => Coordinate) = {
    tris.map{ case (a, b, c) => {
      val pa = trans(a)
      val pb = trans(b)
      val pc = trans(c)

      val d1 = new Coordinate(pb.getX - pa.getX, pb.getY - pa.getY, pb.getZ - pa.getZ)
      val d2 = new Coordinate(pc.getX - pa.getX, pc.getY - pa.getY, pc.getZ - pa.getZ)
      val normal = MatrixUtils.createRealVector(
        Array(d1.getY * d2.getZ - d1.getZ * d2.getY,
              d1.getZ * d2.getX - d1.getX * d2.getZ,
              d1.getX * d2.getY - d1.getY * d2.getX)
      ).unitVector.toArray
      val coeff = -(pa.getX * normal(0) + pa.getY * normal(1) + pa.getZ * normal(2))

      val plane = MatrixUtils.createRealVector(normal :+ coeff)

      plane.outerProduct(plane)
      }}.fold(MatrixUtils.createRealMatrix(4,4))(_.add(_))
  }

  def edgeMatrix(e0: HalfEdge[Int, Int], end: Int, trans: Int => Coordinate) = {
    var e = e0
    var accum = MatrixUtils.createRealMatrix(Array(
      Array[Double](0, 0, 0, 0),
      Array[Double](0, 0, 0, 0),
      Array[Double](0, 0, 0, 0),
      Array[Double](0, 0, 0, 0)
    ))

    do {
      val pa = trans(e.src)
      val pb = trans(e.vert)

      val d1 = new Coordinate(pb.getX - pa.getX, pb.getY - pa.getY, pb.getZ - pa.getZ)
      val d2 = new Coordinate(0, 0, 0.5)
      val normal = MatrixUtils.createRealVector(
        Array(d1.getY * d2.getZ - d1.getZ * d2.getY,
              d1.getZ * d2.getX - d1.getX * d2.getZ,
              d1.getX * d2.getY - d1.getY * d2.getX)
      ).unitVector.toArray
      val coeff = -(pa.getX * normal(0) + pa.getY * normal(1) + pa.getZ * normal(2))

      val plane = MatrixUtils.createRealVector(normal :+ coeff)

      accum = accum.add(plane.outerProduct(plane))
      e = e.next
    } while (e.src != end)

    accum
  }

}
