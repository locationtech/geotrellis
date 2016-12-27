package geotrellis.spark.pointcloud.triangulation

import com.vividsolutions.jts.geom.Coordinate

// import geotrellis.raster._
// import geotrellis.raster.triangulation.DelaunayRasterizer
import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.vector._
import geotrellis.vector.triangulation._

object StitchedDelaunay {

  def directionToVertexOffset(d: Direction) = {
    val increment = Int.MaxValue / 9
    d match {
      case TopLeft => 0
      case Top => increment
      case TopRight => 2 * increment
      case Left => 3 * increment
      case Center => 4 * increment
      case Right => 5 * increment
      case BottomLeft => 6 * increment
      case Bottom => 7 * increment
      case BottomRight => 8 * increment
    }
  }

  def indexToVertex(neighbors: Map[Direction, (BoundaryDelaunay, Extent)])(i: Int) = {
    val increment = Int.MaxValue / 9
    val group = i / increment
    val index = i % increment
    val dir = group match {
      case 0 => TopLeft
      case 1 => Top
      case 2 => TopRight
      case 3 => Left
      case 4 => Center
      case 5 => Right
      case 6 => BottomLeft
      case 7 => Bottom
      case 8 => BottomRight
    }
    neighbors(dir)._1.verts(index)
  }

  /**
   * Given a set of BoundaryDelaunay objects and their non-overlapping boundary
   * extents, each pair associated with a cardinal direction, this function
   * creates a merged representation
   */
  def apply(neighbors: Map[Direction, (BoundaryDelaunay, Extent)]): (TriangleMap, Int => Coordinate) = {
    val vertCount = neighbors.map{ case (_, (bdt, _)) => bdt.verts.size }.reduce(_ + _)
    implicit val allEdges = new HalfEdgeTable(2 * (3 * vertCount - 6))
    val boundaries = neighbors.map{ case (dir, (bdt, _)) => {
      val offset = directionToVertexOffset(dir)
      val reindex = {x: Int => x + offset}
      val edgeoffset = allEdges.appendTable(bdt.navigator, reindex) 
      (dir, (bdt.boundary + edgeoffset, bdt.isLinear))
    }}
    implicit val trans = indexToVertex(neighbors)(_)

    val dirs = Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
    val overlayTris = new TriangleMap

    dirs
      .map{row => row.flatMap{ dir => boundaries.get(dir) }}
      .filter{ row => !row.isEmpty }
      .map{row => row.reduce{ (l, r) => {
        val (left, isLeftLinear) = l
        val (right, isRightLinear) = r
        DelaunayStitcher.merge(left, isLeftLinear, right, isRightLinear, overlayTris)
      }}}

    (overlayTris, trans)
  }
}

// case class StitchedDelaunay(
//   vertMap: Map[Direction, Array[Coordinate]],
//   edges: HalfEdgeTable,
//   triangles: TriangleMap,
//   boundary: Int
// ) {
//   def rasterizeStitchedDelaunay(re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(dt: StitchedDelaunay) = {
//     implicit val trans = { key: StitchedDelaunayKey => vertMap(key.dir)(key.idx) }
//     DelaunayRasterizer.rasterizeTriangles(re, cellType)(triangles.filter{ case (idx, _) => {
//       val (a, b, c) = idx
//       a.dir != b.dir || b.dir != c.dir
//     }})
//   }
// }
