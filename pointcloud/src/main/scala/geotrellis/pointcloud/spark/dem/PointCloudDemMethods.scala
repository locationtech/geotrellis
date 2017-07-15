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

package geotrellis.pointcloud.spark.dem

import io.pdal._

import geotrellis.raster._
import geotrellis.raster.rasterize.triangles.TrianglesRasterizer
import geotrellis.util.MethodExtensions
import geotrellis.vector._
import geotrellis.vector.triangulation.DelaunayTriangulation
import geotrellis.vector.mesh.IndexedPointSet

import com.vividsolutions.jts.geom.Coordinate

trait PointCloudDemMethods extends MethodExtensions[PointCloud] {

  /**
    * Compute the union of this PointCloud and the other one.
    */
  def union(other: Any): PointCloud = {
    val otherCloud = other match {
      case other: PointCloud => other
      case _ => throw new Exception
    }

    require(self.dimTypes == otherCloud.dimTypes)

    PointCloud(self.bytes ++ otherCloud.bytes, self.dimTypes)
  }

  lazy val coords: Array[Coordinate] =
    (0 until self.length).map({ i => new Coordinate(self.getDouble(i, "X"), self.getDouble(i, "Y")) }).toArray
  lazy val xs = (0 until self.length).map({ i => self.getDouble(i, "X") }).toArray
  lazy val ys = (0 until self.length).map({ i => self.getDouble(i, "Y") }).toArray
  lazy val indexMap: Map[(Double, Double), Int] = xs.zip(ys).zipWithIndex.toMap
  lazy val delaunayTriangulation = DelaunayTriangulation(IndexedPointSet(coords))
  val indexToCoord = delaunayTriangulation.pointSet.getCoordinate(_)
  lazy val triangles: Seq[Polygon] = delaunayTriangulation.triangleMap.triangleVertices.toSeq.map {
    case (i, j, k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i))
  }

  def toTile(re: RasterExtent, dimension: String): ArrayTile = {
    val sourceArray = (0 until self.length).map({ i => self.getDouble(i, dimension) }).toArray
    TrianglesRasterizer(re, sourceArray, triangles, indexMap)
  }

}
