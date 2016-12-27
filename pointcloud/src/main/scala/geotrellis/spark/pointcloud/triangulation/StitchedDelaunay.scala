package geotrellis.spark.pointcloud.triangulation

<<<<<<< HEAD
<<<<<<< HEAD
import geotrellis.raster._
import geotrellis.raster.triangulation.DelaunayRasterizer
import geotrellis.spark.buffer.Direction
import geotrellis.vector._
import geotrellis.vector.triangulation._

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}

import scala.annotation.tailrec

case class StitchedDelaunayKey(dir: Direction, idx: Int) extends Ordered[StitchedDelaunayKey] {
  import StitchedDelaunayKey.ord

  def compare(other: StitchedDelaunayKey): Int =
    ord.compare(this, other)
}

object StitchedDelaunayKey {
  import Direction._

  final val directionToInt: Map[Direction, Int] =
    Map(
      (TopLeft, 1),
      (Top, 2),
      (TopRight, 3),
      (Left, 4),
      (Center, 5),
      (Right, 6),
      (BottomLeft, 7),
      (Bottom, 8),
      (BottomRight, 9)
    )

  implicit val ord: Ordering[StitchedDelaunayKey] =
    Ordering.by[StitchedDelaunayKey, (Int, Int)] { k: StitchedDelaunayKey =>
      (directionToInt(k.dir), k.idx)
    }

  implicit def vkeyConvert(di: (Direction, Int)): StitchedDelaunayKey =
    StitchedDelaunayKey(di._1, di._2)
}

object StitchedDelaunay {
  type TKey = (StitchedDelaunayKey, StitchedDelaunayKey, StitchedDelaunayKey)
  type HE = HalfEdge[StitchedDelaunayKey, LightPoint]
  type HEInt = HalfEdge[Int, LightPoint]

  def apply(neighbors: Map[Direction, (FastDelaunay, Extent)]): StitchedDelaunay = {

    val vertMap = neighbors.mapValues{ case (dt, _) => dt.verts }
    val _triangles = collection.mutable.Map.empty[TKey, HE]

    def regularizeTriIndex(idx: TKey) = {
      idx match {
        case (a, b, c) if (a < b && a < c) => (a, b, c)
        case (a, b, c) if (b < a && b < c) => (b, c, a)
        case (a, b, c) => (c, a, b)
      }
    }

    def insertTriangle(idx: TKey, tri: HE): Unit = {
      _triangles += regularizeTriIndex(idx) -> tri
    }

    def insertTriangle2(tri: HE): Unit = {
      val idx = (tri.vert, tri.next.vert, tri.next.next.vert)
      insertTriangle(idx, tri)
    }

    def deleteTriangle(idx: TKey): Unit = {
      _triangles -= regularizeTriIndex(idx)
    }

    def deleteTriangle2(tri: HE): Unit = {
      val idx = (tri.vert, tri.next.vert, tri.next.next.vert)
      deleteTriangle(idx)
    }

    def containsTriangle(idx: TKey): Boolean = {
      _triangles.contains(regularizeTriIndex(idx))
    }

    def containsTriangle2(tri: HE): Boolean = {
      val idx = (tri.vert, tri.next.vert, tri.next.next.vert)
      containsTriangle(idx)
    }

    def joinTriangles[V,F](t1: HalfEdge[V,F], t2: HalfEdge[V,F]): Unit = {
      assert(t1.src == t2.vert)
      assert(t2.src == t1.vert)
      assert(t1.flip.face == None)
      assert(t2.flip.face == None)

      t1.flip.prev.next = t2.flip.next
      t2.flip.prev.next = t1.flip.next
      t1.flip = t2
      t2.flip = t1
    }

    def distanceToExtent(lp: LightPoint, ex: Extent): Double = {
      val ppd = new PointPairDistance
      DistanceToPoint.computeDistance(ex.toPolygon.jtsGeom, lp.toPoint.jtsGeom.getCoordinate, ppd)
      ppd.getDistance
    }

    def circumcircleLeavesExtent(dir: Direction, extent: Extent)(tri: HEInt): Boolean = {
      val center = tri.face.get
      val radius = center.distance(vertMap(dir).apply(tri.vert))
      val ppd = new PointPairDistance
      val closest = DistanceToPoint.computeDistance(extent.toPolygon.jtsGeom, center.toPoint.jtsGeom.getCoordinate, ppd)

      ppd.getDistance < radius
    }

    def extractBoundingMesh(dir: Direction, extent: Extent, dt: FastDelaunay): HE = {
      val boundary = dt.boundary

      def lookupTriangle(tri: HEInt): Option[HE] = {
        val idx = regularizeTriIndex((dir, tri.vert), (dir, tri.next.vert), (dir, tri.next.next.vert))
        _triangles.get(idx) match {
          case Some(base) => {
            var e = base
            do {
              if (e.vert.idx == tri.vert) {
                return Some(e)
              }
              e = e.next
            } while (e != base)
              Some(base)
          }
          case None => {
            None
          }
        }
      }

      def copyConvertEdge(e: HEInt): HE = HalfEdge[StitchedDelaunayKey, LightPoint]((dir, e.src), (dir, e.vert))

      def copyConvertLinearBound(): HE = {
        val hes = collection.mutable.Map.empty[(Int, Int), HE]
        var e = boundary
        do {
          val edge = new HalfEdge[StitchedDelaunayKey, LightPoint]((dir, e.vert), null, null, None)
          hes += (e.src, e.vert) -> edge
          e = e.next
        } while (e != boundary)
          do {
            val edge = hes((e.src, e.vert))
            val flip = hes((e.vert, e.src))
            edge.flip = flip
            flip.flip = edge
            edge.next = hes((e.vert, e.next.vert))
            e = e.next
          } while (e != boundary)
            hes((boundary.src, boundary.vert))
      }

      def copyConvertBoundingLoop(orig: HEInt, newBound: HE): HE = {
        var copy = if (orig == boundary) newBound else copyConvertEdge(orig)
        if (orig.next != boundary) {
          copy.next = copyConvertBoundingLoop(orig.next, newBound)
        } else {
          copy.next = newBound
        }
        copy.next.flip.next = copy.flip
        copy
      }

      def copyConvertTriangle(tri: HEInt): HE = {
        val copy = HalfEdge(Seq[StitchedDelaunayKey]((dir, tri.vert), (dir, tri.next.vert), (dir, tri.next.next.vert)), tri.face.get).next.flip
        insertTriangle2(copy)
        copy
      }

      def recursiveAddTris(e0: HEInt, opp0: HE): Unit = {
        val workQueue = collection.mutable.Queue( (e0, opp0) )

        while (!workQueue.isEmpty) {
          val (e, opp) = workQueue.dequeue
          if (e.face != None && opp.flip.face == None) {
            lookupTriangle(e) match {
              case Some(tri) => {
                joinTriangles(opp, tri)
              }
              case None => {
                if (circumcircleLeavesExtent(dir, extent)(e)) {
                  val tri = copyConvertTriangle(e)
                  joinTriangles(opp, tri)
                  workQueue.enqueue( (e.next.flip, tri.next) )
                  workQueue.enqueue( (e.next.next.flip, tri.next.next) )
                }
              }
            }
          }
        }
      }


      def copyConvertBoundingTris(): HE = {
        val newBound = copyConvertBoundingLoop(boundary, copyConvertEdge(boundary))
        var e = boundary
        var ne = newBound
        val boundingTris = collection.mutable.Set.empty[HEInt]
        do {
          recursiveAddTris(e.flip, ne)
          e = e.next
          ne = ne.next
        } while (e != boundary)

        assert(ne == newBound)
        newBound
      }

      if (dt.isLinear)
        copyConvertLinearBound
      else
        copyConvertBoundingTris
    }

    def zipper(left0: HE, isLeftLinear: Boolean, lExtent: Extent, right0: HE, isRightLinear: Boolean, rExtent: Extent): (HE, Boolean) = {
      implicit def verts(key: StitchedDelaunayKey): LightPoint = {
        vertMap(key.dir)(key.idx)
      }
      import Predicates._

      def advance(e0: HE): HE = {
        var e = e0.next
        while (!isCorner(e))
          e = e.next
        e
      }
      def reverse(e0: HE): HE = {
        var e = e0.prev
        while (!isCorner(e))
          e = e.prev
        e
      }
      def advanceIfNotCorner(e0: HE) = {
        var e = e0
        while (!isCorner(e))
          e = e.next
        e
      }

      var left = advanceIfNotCorner(left0)
      var right = advanceIfNotCorner(right0)

      if (isLeftLinear) {
        // Pick the endpoint closest to the opposing extent
        val other = advance(left)
        if (distanceToExtent(verts(other.src), rExtent) < distanceToExtent(verts(left.src), rExtent))
          left = other
      }
      if (isRightLinear) {
        val other = advance(right)
        if (distanceToExtent(verts(other.src), lExtent) < distanceToExtent(verts(right.src), lExtent))
          right = other
      }

      // compute the lower common tangent of left and right
      var continue = true
      var base: HE = null
      while (continue) {
        base = HalfEdge[StitchedDelaunayKey, LightPoint](right.src, left.src)
        if (isLeftOf(base, left.vert)) {
          // left points to a vertex that is to the left of
          // base, so move base to left.next
          left = advance(left)
        } else if (isLeftOf(base, right.vert)) {
          // right points to a point that is left of base,
          // so keep walking right
          right = advance(right)
        } else if (isLeftOf(base, left.prev.src)) {
          // Left's previous source is left of base,
          // so this base would break tangency. Move
          // back to previous left.
          left = reverse(left)
        } else if (isLeftOf(base, right.prev.src)) {
          // Right's previous source is left of base,
          // so this base would break tangency. Move
          // back to previous right.
          right = reverse(right)
        } else if(!isRightLinear && isCollinear(base, right.vert)) {
          // Right is collinear with base and its dest is closer to left; move right forward.
          right = advance(right)
        } else if (!isLeftLinear && isCollinear(base, left.prev.src)) {
          // Left's previous src is collinear with base and is closer to right; move left back.
          left = reverse(left)
        } else {
          continue = false
        }
      }

      base.next = left
      base.flip.next = right
      left.prev.next = base.flip
      right.prev.next = base

      // If linear joins to linear, check that the current state isn't already done (linear result)
      if (isLeftLinear && isRightLinear) {
        val b0 = verts(base.src)
        val b1 = verts(base.vert)
        val l = verts(base.next.vert)
        val r = verts(base.flip.next.vert)
        if (isCollinear(b0, b1, l)  && isCollinear(b0, b1, r)) {
          return (base.flip, true)
        }
      }

      continue = true
      while (continue) {
        var lcand = base.flip.rotCCWSrc
        var rcand = base.rotCWSrc

        // Find left side candidate edge for extending the fill triangulation
        if (isCCW(lcand.vert, base.vert, base.src)) {
          while (inCircle((base.vert, base.src, lcand.vert), lcand.rotCCWSrc.vert)) {
            val e = lcand.rotCCWSrc
            deleteTriangle2(lcand)
            lcand.rotCCWDest.next = lcand.next
            lcand.prev.next = lcand.flip.next
            lcand = e
          }
        }

        // Find right side candidate edge for extending the fill triangulation
        if (isCCW(rcand.vert, base.vert, base.src)) {
          while (inCircle((base.vert, base.src, rcand.vert), rcand.rotCWSrc.vert)) {
            val e = rcand.rotCWSrc
            deleteTriangle2(rcand.flip)
            base.flip.next = rcand.rotCWSrc
            rcand.rotCCWDest.next = rcand.next
            rcand = e
          }
        }

        if (!isCCW(lcand.vert, base.vert, base.src) && !isCCW(rcand.vert, base.vert, base.src)) {
          // no further Delaunay triangles to add
          continue = false
        } else {
          if (!isCCW(lcand.vert, base.vert, base.src) ||
            (isCCW(rcand.vert, base.vert, base.src) &&
              inCircle((lcand.vert, lcand.src, rcand.src), rcand.vert)))
          {
            // form new triangle from rcand and base
            val e = HalfEdge[StitchedDelaunayKey, LightPoint](rcand.vert, base.vert)
            e.flip.next = rcand.next
            e.next = base.flip
            rcand.next = e
            lcand.flip.next = e.flip
            base = e

          } else {
            // form new triangle from lcand and base
            val e = HalfEdge[StitchedDelaunayKey, LightPoint](base.src, lcand.vert)
            lcand.rotCCWDest.next = e.flip
            e.next = lcand.flip
            e.flip.next = rcand
            base.flip.next = e
            base = e
          }

          // Tag edges with the center of the new triangle's circumscribing circle
          val c = circleCenter(base.vert, base.next.vert, base.next.next.vert)
          base.face = Some(c)
          base.next.face = Some(c)
          base.next.next.face = Some(c)
          insertTriangle2(base)
        }
      }

      (base.flip.next, false)
    }

    def stitch(): HE = {
      def toMesh(dir: Direction): Option[(HE, Boolean, Extent)] = {
        neighbors.get(dir)
          .map{ case (dt, extent) =>
            val boundingMesh = extractBoundingMesh(dir, extent, dt)
            (boundingMesh, dt.isLinear, extent)
          }
      }

      def joinMeshes(in1: (HE, Boolean, Extent), in2: (HE, Boolean, Extent)): (HE, Boolean, Extent) = {
        val (left, leftLinear, leftExtent) = in1
        val (right, rightLinear, rightExtent) = in2
        val (joined, linearResult) = zipper(left, leftLinear, leftExtent, right, rightLinear, rightExtent)
        (joined, linearResult, leftExtent.expandToInclude(rightExtent))
      }

      import Direction._
      val rows =
        Seq(
          Seq(TopLeft, Top, TopRight),
          Seq(Left, Center, Right),
          Seq(BottomLeft, Bottom, BottomRight)
        )

      val combined =
        rows
          .map{_.flatMap(toMesh(_))}
          .filter(!_.isEmpty)
          .map{_.reduce(joinMeshes(_,_))}
          .reduce(joinMeshes(_, _))

      combined._1
    }

    val boundary = stitch()
    val triangles = _triangles.toMap

    StitchedDelaunay(vertMap, triangles, boundary)
  }
}

import StitchedDelaunay.{TKey, HE, HEInt}

case class StitchedDelaunay(
  vertMap: Map[Direction, Array[LightPoint]],
  triangles: Map[TKey, HE],
  boundary: HE
) {
  def rasterizeStitchedDelaunay(re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(dt: StitchedDelaunay) = {
    implicit val trans = { key: StitchedDelaunayKey => vertMap(key.dir)(key.idx) }
    DelaunayRasterizer.rasterizeTriangles(re, cellType)(triangles.filter{ case (idx, _) => {
      val (a, b, c) = idx
      a.dir != b.dir || b.dir != c.dir
    }})
  }
}
=======
=======
>>>>>>> e98f6d3... Finished base functionality; testing still required
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
<<<<<<< HEAD
>>>>>>> e98f6d3... Finished base functionality; testing still required
=======
>>>>>>> e98f6d3... Finished base functionality; testing still required
