package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate
import geotrellis.util.Direction
import geotrellis.util.Direction._
import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT

object StitchedDelaunay {

  def combinedPointSet(
    center: DelaunayTriangulation, 
    neighbors: Map[Direction, (BoundaryDelaunay, Extent)]
  ): (Int, IndexedPointSet, Map[Direction, Map[Int, Int]]) = {
    val regions: Seq[Direction] = Seq(Center, Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft).filter(neighbors.keys.toSet.contains(_))
    val ptCounts: Seq[Int] = regions.map { dir => if (dir == Center) center.pointSet.length else neighbors.get(dir).map{_._1.pointSet.length}.getOrElse(0) }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.reduce(_+_)
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] = {
      val het = if (dir == Center)
        center.halfEdgeTable
      else
        neighbors(dir)._1.halfEdgeTable

      het.allVertices.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap
    }
    val vtrans = regions.map { dir => (dir, vertIndices(dir)) }.toMap

    regions.foreach{ dir => 
      val mapping = vtrans(dir)
      val pointSet = if (dir == Center) center.pointSet else neighbors(dir)._1.pointSet
      mapping.foreach { case (orig, newix) => assert(points(newix) == null) ; points(newix) = pointSet(orig) }
    }

    (vertCount, IndexedPointSet(points), vtrans)
  }

  def combinedPointSet(neighbors: Map[Direction, (BoundaryDelaunay, Extent)]): (Int, IndexedPointSet, Map[Direction, Map[Int, Int]]) = {
    val regions: Seq[Direction] = Seq(Center, Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft)
    val ptCounts: Seq[Int] = regions.map { dir => neighbors.get(dir).map{_._1.pointSet.length}.getOrElse(0) }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.reduce(_+_)
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] =
      neighbors(dir)._1.halfEdgeTable.allVertices.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap

    val vtrans = regions.map { dir => (dir, vertIndices(dir)) }.toMap

    regions.foreach{ dir => 
      val mapping = vtrans(dir)
      val pointSet = neighbors(dir)._1.pointSet
      mapping.foreach { case (orig, newix) => assert(points(newix) == null) ; points(newix) = pointSet(orig) }
    }

    (vertCount, IndexedPointSet(points), vtrans)
  }

  def combinedPointSet(neighbors: Map[Direction, (DelaunayTriangulation, Extent)])(implicit dummy: DummyImplicit): (Int, IndexedPointSet, Map[Direction, Map[Int, Int]]) = {
    val regions: Seq[Direction] = Seq(Center, Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft)
    val ptCounts: Seq[Int] = regions.map { dir => neighbors.get(dir).map{_._1.pointSet.length}.getOrElse(0) }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.reduce(_+_)
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] =
      neighbors(dir)._1.halfEdgeTable.allVertices.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap

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
      val reindex = vtrans(dir).apply(_)
      val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, vtrans(dir)(_))
      (dir, (bdt.boundary + edgeoffset, bdt.isLinear))
    }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
      .map{row => row.reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
        result
      }}}
      .reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
      }}

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, allPoints, overlayTris)
  }

  def apply(neighbors: Map[Direction, (DelaunayTriangulation, Extent)], debug: Boolean)(implicit dummy: DummyImplicit): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))

    val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
      val reindex = vtrans(dir).apply(_)
      val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, vtrans(dir)(_))
      (dir, (bdt.boundary + edgeoffset, bdt.isLinear))
    }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
      .map{row => row.reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
        result
      }}}
      .reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
      }}

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, allPoints, overlayTris)
  }

  def apply(center: DelaunayTriangulation, neighbors: Map[Direction, (BoundaryDelaunay, Extent)], debug: Boolean): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(center, neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))
    val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
      val reindex = vtrans(dir).apply(_)
      if (dir == Center) {
        val edgeoffset = allEdges.appendTable(center.halfEdgeTable, vtrans(dir)(_))
        (dir, (center.boundary + edgeoffset, center.isLinear))
      } else {
        val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, vtrans(dir)(_))
        (dir, (bdt.boundary + edgeoffset, bdt.isLinear))
      }
    }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
      .map{row => row.reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
        result
      }}}
      .reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
      }}

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, allPoints, overlayTris)
  }

}

case class StitchedDelaunay(
  indexToCoord: Int => Coordinate,
  halfEdgeTable: HalfEdgeTable,
  private val pointSet: IndexedPointSet,
  private val fillTriangles: TriangleMap
) {
  def triangles(): Seq[(Int, Int, Int)] = fillTriangles.getTriangles.keys.toSeq

  def writeWKT(wktFile: String) = {
    val mp = MultiPolygon(triangles.map{ case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
    val wktString = WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }
}
