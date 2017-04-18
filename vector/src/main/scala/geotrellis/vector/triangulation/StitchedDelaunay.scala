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
      // val het = if (dir == Center)
      //   center.halfEdgeTable
      // else
      //   neighbors(dir)._1.halfEdgeTable

      // het.allVertices.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap

      val liveVs = if (dir == Center)
        center.liveVertices
      else
        neighbors(dir)._1.liveVertices

      liveVs.toSeq.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap
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
    val regions: Seq[Direction] = Seq(Center, Left, BottomLeft, Bottom, BottomRight, Right, TopRight, Top, TopLeft).filter(neighbors.keys.toSet.contains(_))
    val ptCounts: Seq[Int] = regions.map { dir => neighbors.get(dir).map{_._1.pointSet.length}.getOrElse(0) }
    val countMap = regions.zip(ptCounts).toMap
    val startIndices: Map[Direction, Int] = regions.zip(ptCounts.foldLeft(Seq(0)){ (accum, count) => accum :+ (accum.last + count) }).toMap
    val vertCount = ptCounts.reduce(_+_)
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] =
      // neighbors(dir)._1.halfEdgeTable.allVertices.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap
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
    val vertCount = ptCounts.reduce(_+_)
    val points = Array.ofDim[Coordinate](vertCount)

    def vertIndices(dir: Direction): Map[Int, Int] =
      // neighbors(dir)._1.halfEdgeTable.allVertices.zip(0 + startIndices(dir) to countMap(dir) + startIndices(dir)).toMap
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
    // println(s"Found $vertCount points in input, allocating for ${2 * (3 * vertCount - 6)} edges")
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))

    // val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
    //   val reindex = vtrans(dir).apply(_)
    //   val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, vtrans(dir)(_))
    //   (dir, (bdt.boundary + edgeoffset, bdt.isLinear))
    // }}
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

    // dirs
    //   .map{row => row.flatMap{ dir => boundaries.get(dir) }}
    //   .filter{ row => !row.isEmpty }
    //   .map{row => row.reduce{ (l, r) => {
    //     val (left, isLeftLinear) = l
    //     val (right, isRightLinear) = r
    //     val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
    //     result
    //   }}}
    //   .reduce{ (l, r) => {
    //     val (left, isLeftLinear) = l
    //     val (right, isRightLinear) = r
    //     stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
    //   }}
    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
      .map{row => row.reduce{ (l, r) => (l, r) match {
        case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
          // println("Merging two normal triangulations (row)")
          scala.Left(stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug))
        case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
          // println("Merging a single point to a normal triangulation (row)")
          scala.Left(stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris))
        case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
          // println("Merging a single point to a normal triangulation (row)")
          scala.Left(stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris))
        case (scala.Right(v1), scala.Right(v2)) =>
          // println("Merging two single point 'triangulations' (row)")
          scala.Left((allEdges.createHalfEdges(v1, v2), true))
      }}}
      .reduce{ (l, r) => (l, r) match {
        case (scala.Left((left, isLeftLinear)), scala.Left((right, isRightLinear))) =>
          // println("Merging two normal triangulations (column)")
          scala.Left(stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug))
        case (scala.Left((left, isLeftLinear)), scala.Right(vertex)) =>
          // println("Merging a single point to a normal triangulation (column)")
          scala.Left(stitcher.joinToVertex(left, isLeftLinear, vertex, overlayTris))
        case (scala.Right(vertex), scala.Left((right, isRightLinear))) =>
          // println("Merging a single point to a normal triangulation (column)")
          scala.Left(stitcher.joinToVertex(right, isRightLinear, vertex, overlayTris))
        case (scala.Right(v1), scala.Right(v2)) =>
          // println("Merging two single point 'triangulations' (column)")
          scala.Left((allEdges.createHalfEdges(v1, v2), true))
      }}

    // allEdges.navigate(0, allPoints.getCoordinate(_), Map.empty)
    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, allPoints, overlayTris)
  }

  def apply(neighbors: Map[Direction, (DelaunayTriangulation, Extent)], debug: Boolean)(implicit dummy: DummyImplicit): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))

    // val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
    //   val reindex = vtrans(dir).apply(_)
    //   val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, vtrans(dir)(_))
    //   (dir, (bdt.boundary + edgeoffset, bdt.isLinear))
    // }}
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

    // dirs
    //   .map{row => row.flatMap{ dir => boundaries.get(dir) }}
    //   .filter{ row => !row.isEmpty }
    //   .map{row => row.reduce{ (l, r) => {
    //     val (left, isLeftLinear) = l
    //     val (right, isRightLinear) = r
    //     val result = stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
    //     result
    //   }}}
    //   .reduce{ (l, r) => {
    //     val (left, isLeftLinear) = l
    //     val (right, isRightLinear) = r
    //     stitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris, debug)
    //   }}

    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
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

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, allPoints, overlayTris)
  }

  def apply(center: DelaunayTriangulation, neighbors: Map[Direction, (BoundaryDelaunay, Extent)], debug: Boolean): StitchedDelaunay = {
    val (vertCount, allPoints, vtrans) = combinedPointSet(center, neighbors)
    val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))
    val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
      val reindex = vtrans(dir).apply(_)
      if (dir == Center) {
        val edgeoffset = allEdges.appendTable(center.halfEdgeTable, reindex)
        val handle: Either[(Int, Boolean), Int] = 
          if (center.liveVertices.size == 1) 
            scala.Right(reindex(center.liveVertices.toSeq(0)))
          else 
            scala.Left((center.boundary + edgeoffset, center.isLinear))
        (dir, handle)
      } else {
        val edgeoffset = allEdges.appendTable(bdt.halfEdgeTable, vtrans(dir)(_))
        val handle: Either[(Int, Boolean), Int] = 
          if (bdt.liveVertices.size == 1) 
            scala.Right(bdt.liveVertices.toSeq(0))
          else 
            scala.Left((bdt.boundary + edgeoffset, bdt.isLinear))
        (dir, handle)
      }
    }}

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap(allEdges)
    val stitcher = new DelaunayStitcher(allPoints, allEdges)

    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
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

    new StitchedDelaunay(allPoints.getCoordinate(_), allEdges, allPoints, overlayTris)
  }

}

case class StitchedDelaunay(
  indexToCoord: Int => Coordinate,
  halfEdgeTable: HalfEdgeTable,
  val pointSet: IndexedPointSet,
  private val fillTriangles: TriangleMap
) {
  def triangles(): Seq[(Int, Int, Int)] = fillTriangles.getTriangles.keys.toSeq

  def writeWKT(wktFile: String) = {
    val mp = MultiPolygon(triangles.map{ case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
    val wktString = WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }
}
