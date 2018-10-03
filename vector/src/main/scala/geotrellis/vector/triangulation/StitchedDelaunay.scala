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
import geotrellis.util.Direction
import geotrellis.util.Direction._
import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT
import geotrellis.vector.mesh.{HalfEdgeTable, IndexedPointSet}

object StitchedDelaunay {

  def combinedPointSet(
    center: DelaunayTriangulation,
    neighbors: Map[Direction, (BoundaryDelaunay, Extent)]
  ): (Int, IndexedPointSet, Map[Direction, Map[Int, Int]]) = {
    val regions: Seq[Direction] = Center +: Seq(Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft).filter(neighbors.keys.toSet.contains(_))
    val ptCounts: Seq[Int] = regions.map { dir => if (dir == Center) center.pointSet.length else neighbors(dir)._1.pointSet.length }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.sum
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] = {
      val liveVs = if (dir == Center)
        center.liveVertices
      else
        neighbors(dir)._1.liveVertices

      liveVs.zip(startIndices(dir) to startIndices(dir) + countMap(dir)).toMap
    }
    val vtrans = regions.map { dir => (dir, vertIndices(dir)) }.toMap

    regions.foreach{ dir =>
      val mapping = vtrans(dir)
      val pointSet = if (dir == Center) center.pointSet else neighbors(dir)._1.pointSet
      mapping.foreach { case (orig, newix) =>
        assert(points(newix) == null)
        points(newix) = pointSet(orig)
      }
    }

    (vertCount, IndexedPointSet(points), vtrans)
  }

  def combinedPointSet(neighbors: Map[Direction, (BoundaryDelaunay, Extent)]): (Int, IndexedPointSet, Map[Direction, Map[Int, Int]]) = {
    val regions: Seq[Direction] = Seq(Center, Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft).filter(neighbors.keys.toSet.contains(_))
    val ptCounts: Seq[Int] = regions.map { dir => neighbors.get(dir).map{_._1.pointSet.length}.getOrElse(0) }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.sum
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] =
      neighbors(dir)._1.liveVertices.toSeq.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap

    val vtrans = regions.map { dir => (dir, vertIndices(dir)) }.toMap

    regions.foreach{ dir =>
      val mapping = vtrans(dir)
      val pointSet = neighbors(dir)._1.pointSet
      mapping.foreach { case (orig, newix) => assert(points(newix) == null) ; points(newix) = pointSet(orig) }
    }

    (vertCount, IndexedPointSet(points), vtrans)
  }

  def combinedPointSet(neighbors: Map[Direction, (DelaunayTriangulation, Extent)])(implicit dummy: DummyImplicit): (Int, IndexedPointSet, Map[Direction, Map[Int, Int]]) = {
    val regions: Seq[Direction] = Seq(Center, Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft).filter(neighbors.keys.toSet.contains(_))
    val ptCounts: Seq[Int] = regions.map { dir => neighbors.get(dir).map{_._1.pointSet.length}.getOrElse(0) }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.sum
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] =
      neighbors(dir)._1.liveVertices.toSeq.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap

    val vtrans = regions.map { dir => (dir, vertIndices(dir)) }.toMap

    regions.foreach{ dir =>
      val mapping = vtrans(dir)
      val pointSet = neighbors(dir)._1.pointSet
      mapping.foreach { case (orig, newix) => assert(points(newix) == null) ; points(newix) = pointSet(orig) }
    }

    (vertCount, IndexedPointSet(points), vtrans)
  }

  /**
    * Given a set of BoundaryDelaunay objects and their non-overlapping boundary
    * extents, each pair associated with a cardinal direction, this function
    * creates a merged representation
    */
  def apply(neighbors: Map[Direction, (BoundaryDelaunay, Extent)], debug: Boolean = false): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))

    val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
      val reindex = vtrans(dir)(_)
      val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, reindex)
      val handle: Either[(Int, Boolean), Int] =
        if (bdt.liveVertices.size == 1)
          scala.Right(reindex(bdt.liveVertices.toSeq(0)))
        else
          scala.Left((bdt.boundary + edgeoffset, bdt.isLinear))
      (dir, handle)
    }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    val bound =
      dirs
        .map{row => row.flatMap{ dir => boundaries.get(dir) }}
        .filter{ row => row.nonEmpty }
        .map{row => row.reduce{ (l, r) => (l, r) match {
          case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
            scala.Left(stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug))
          case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
            scala.Left(stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris))
          case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
            scala.Left(stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris))
          case (scala.Right(v1), scala.Right(v2)) =>
            scala.Left((allEdges.createHalfEdges(v1, v2), true))
        }}}
        .reduce{ (l, r) => (l, r) match {
          case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
            scala.Left(stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug))
          case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
            scala.Left(stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris))
          case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
            scala.Left(stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris))
          case (scala.Right(v1), scala.Right(v2)) =>
            scala.Left((allEdges.createHalfEdges(v1, v2), true))
        }}

    val boundary = bound match {
      case scala.Left((bnd, _)) => bnd
      case scala.Right(_) => -1
    }

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, boundary, allPoints, overlayTris)
  }

  def apply(neighbors: Map[Direction, (DelaunayTriangulation, Extent)], debug: Boolean)(implicit dummy: DummyImplicit): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))

    val boundaries = neighbors.map{ case (dir, (dt, _)) => {
      val reindex = vtrans(dir).apply(_)
      val edgeoffset = allEdges.appendTable(dt.halfEdgeTable, reindex)
      val handle: Either[(Int, Boolean), Int] =
        if (dt.liveVertices.size == 1)
          scala.Right(reindex(dt.liveVertices.toSeq(0)))
        else
          scala.Left((dt.boundary + edgeoffset, dt.isLinear))
      (dir, handle)
    }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    val bound =
      dirs
        .map{row => row.flatMap{ dir => boundaries.get(dir) }}
        .filter{ row => row.nonEmpty }
        .map{row => row.reduce{ (l, r) => (l, r) match {
          case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
            scala.Left(stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug))
          case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
            scala.Left(stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris))
          case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
            scala.Left(stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris))
          case (scala.Right(v1), scala.Right(v2)) =>
            scala.Left((allEdges.createHalfEdges(v1, v2), true))
        }}}
    .reduce{ (l, r) => (l, r) match {
      case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
        scala.Left(stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug))
      case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
        scala.Left(stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris))
      case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
        scala.Left(stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris))
      case (scala.Right(v1), scala.Right(v2)) =>
        scala.Left((allEdges.createHalfEdges(v1, v2), true))
    }}

    val boundary = bound match {
      case scala.Left((bnd, _)) => bnd
      case scala.Right(_) => -1
    }

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, boundary, allPoints, overlayTris)
  }

  def apply(center: DelaunayTriangulation, neighbors: Map[Direction, (BoundaryDelaunay, Extent)], debug: Boolean): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(center, neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))

    val boundaries =
      neighbors
        .filter{ case (dir, (bdt, _)) => {
          if (dir == Center) {
            center.liveVertices.nonEmpty
          } else {
            bdt.liveVertices.nonEmpty
          }
        }}.map{ case (dir, (bdt, _)) => {
          val reindex = vtrans(dir)(_)
          if (dir == Center) {
            val edgeoffset = allEdges.appendTable(center.halfEdgeTable, reindex)
            val handle: Either[(Int, Boolean), Int] =
              if (center.liveVertices.size == 1)
                scala.Right(reindex(center.liveVertices.head))
              else {
                scala.Left((center.boundary + edgeoffset, center.isLinear))
              }
            (dir, handle)
          } else {
            val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, reindex)
            val handle: Either[(Int, Boolean), Int] =
              if (bdt.liveVertices.size == 1)
                scala.Right(reindex(bdt.liveVertices.head))
              else {
                scala.Left((bdt.boundary + edgeoffset, bdt.isLinear))
              }
            (dir, handle)
          }
        }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    var i = 0

    val joinedRows =
      dirs
        .map{row => row.flatMap{ dir => boundaries.get(dir) }}
        .filter{ row => row.nonEmpty }
        .map{row => row.reduce{ (l, r) =>
          val result: Either[(Int, Boolean), Int] = (l, r) match {
            case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
              val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
              if (debug) {
                println("Merging two normal triangulations (row 0)")
                val mp = MultiPolygon(overlayTris.triangleVertices.map{ case (i,j,k) => Polygon(allPoints(i), allPoints(j), allPoints(k), allPoints(i)) })
                new java.io.PrintWriter(s"stitch_r${i}.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
              }
              scala.Left(result)
            case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
              val result = stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris)
              if (debug) {
                println("Merging a single point to a normal triangulation (row 1)")
                val mp = MultiPolygon(overlayTris.triangleVertices.map{ case (i,j,k) => Polygon(allPoints(i), allPoints(j), allPoints(k), allPoints(i)) })
                new java.io.PrintWriter(s"stitch_r${i}.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
              }
              scala.Left(result)
            case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
              val result = stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris)
              if (debug) {
                println("Merging a single point to a normal triangulation (row 2)")
                val mp = MultiPolygon(overlayTris.triangleVertices.map{ case (i,j,k) => Polygon(allPoints(i), allPoints(j), allPoints(k), allPoints(i)) })
                new java.io.PrintWriter(s"stitch_r${i}.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
              }
              scala.Left(result)
            case (scala.Right(v1), scala.Right(v2)) =>
              if (debug) println("Merging two single point 'triangulations' (row 3)")
              scala.Left((allEdges.createHalfEdges(v1, v2), true))
          }
          i += 1
          result
        }}

    val bound =
      if (joinedRows.isEmpty) {
        scala.Left((-1, true))
      } else {
        joinedRows
          .reduce{ (l, r) => {
            val result: Either[(Int, Boolean), Int] = (l, r) match {
              case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
                val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris)
                if (debug) {
                  println("Merging two normal triangulations (column)")
                  val mp = MultiPolygon(overlayTris.triangleVertices.map{ case (i,j,k) => Polygon(allPoints(i), allPoints(j), allPoints(k), allPoints(i)) })
                  new java.io.PrintWriter(s"stitch_c${i}.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
                }
                scala.Left(result)
              case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
                val result = stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris)
                if (debug) {
                  println("Merging a single point to a normal triangulation (column)")
                  val mp = MultiPolygon(overlayTris.triangleVertices.map{ case (i,j,k) => Polygon(allPoints(i), allPoints(j), allPoints(k), allPoints(i)) })
                  new java.io.PrintWriter(s"stitch_c${i}.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
                }
                scala.Left(result)
              case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
                val result = stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris)
                if (debug) {
                    println("Merging a single point to a normal triangulation (column)")
                  val mp = MultiPolygon(overlayTris.triangleVertices.map{ case (i,j,k) => Polygon(allPoints(i), allPoints(j), allPoints(k), allPoints(i)) })
                  new java.io.PrintWriter(s"stitch_c${i}.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
                }
                scala.Left(result)
              case (scala.Right(v1), scala.Right(v2)) =>
                if (debug) println("Merging two single point 'triangulations' (column)")
                scala.Left((allEdges.createHalfEdges(v1, v2), true))
            }
            i += 1
            result
          }}
      }

    val boundary = bound match {
      case scala.Left((bnd, _)) => bnd
      case scala.Right(_) => -1
    }

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, boundary, allPoints, overlayTris)
  }

}

case class StitchedDelaunay(
  indexToCoord: Int => Coordinate,
  halfEdgeTable: HalfEdgeTable,
  boundary: Int,
  pointSet: IndexedPointSet,
  fillTriangles: TriangleMap
) extends Serializable {
  def triangles(): Seq[(Int, Int, Int)] = fillTriangles.getTriangles.keys.toSeq

  def writeWKT(wktFile: String) = {
    val mp = MultiPolygon(triangles.map { case (i, j, k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
    val wktString = WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }
}
