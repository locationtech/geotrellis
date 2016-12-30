// TODO: Should this be broken out?
// package geotrellis.spark.pointcloud.triangulation

// import geotrellis.spark.pointcloud._
// import geotrellis.vector._
// import geotrellis.vector.triangulation._
// import geotrellis.vector.io._

// import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
// import com.vividsolutions.jts.geom.Coordinate

// import scala.collection.mutable

// case class BoundingMesh(
//   extent: Extent,
//   boundary: HalfEdge[Int, Unit],
//   triangles: Map[(Int, Int, Int), HalfEdge[Int, Unit]],
//   isLinear: Boolean
// )

// object BoundingMesh {
//   def apply(triangulation: PointCloudTriangulation, extent: Extent): BoundingMesh = {
//     import triangulation._
//     val pcp = new PointSetPredicates(pointSet, halfEdgeTable)
//     import pcp._
//     import halfEdgeTable._

//     val _triangles =
//       mutable.Map.empty[(Int, Int, Int), HalfEdge[Int, Unit]]

//     val outerEdges = collection.mutable.Set[(Int, Int)]()

//     val boundingEdge = {
//       def lookupTriangle(tri: Int): Option[HalfEdge[Int, Unit]] = {
//         _triangles.get((getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri))))) match {
//           case Some(base) => {
//             var e = base
//             do {
//               if (e.vert == getVert(tri)) {
//                 return Some(e)
//               }
//               e = e.next
//             } while (e != base)

//             Some(base)
//           }
//           case None => {
//             None
//           }
//         }
//       }

//       def copyConvertEdge(e: Int): HalfEdge[Int, Unit] =
//         HalfEdge[Int, Unit](getSrc(e), getVert(e))

//       def copyConvertLinearBound(): HalfEdge[Int, Unit] = {
//         val hes = collection.mutable.Map.empty[(Int, Int), HalfEdge[Int, Unit]]
//         var e = boundary
//         do {
//           val edge = new HalfEdge[Int, Unit](getVert(e), null, null, None)
//           hes += (getSrc(e), getVert(e)) -> edge
//           e = getNext(e)
//         } while (e != boundary)
//           do {
//             val edge = hes((getSrc(e), getVert(e)))
//             val flip = hes((getVert(e), getSrc(e)))
//             edge.flip = flip
//             flip.flip = edge
//             edge.next = hes((getVert(e), getVert(getNext(e))))
//             e = getNext(e)
//           } while (e != boundary)
//             hes((getSrc(boundary), getVert(boundary)))
//       }

//       def copyConvertBoundingLoop(orig: Int, newBound: HalfEdge[Int, Unit]): HalfEdge[Int, Unit] = {
//         // Record orig as a outer bound
//         outerEdges += ((getSrc(orig), getVert(orig)))

//         var copy =
//           if (orig == boundary) newBound else copyConvertEdge(orig)

//         if (getNext(orig) != boundary) {
//           copy.next = copyConvertBoundingLoop(getNext(orig), newBound)
//         } else {
//           copy.next = newBound
//         }
//         copy.next.flip.next = copy.flip
//         copy
//       }

//       def copyConvertTriangle(tri: Int): HalfEdge[Int, Unit] = {
//         val copy =
//           HalfEdge(
//             Seq(getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri)))),
//             ()
//           ).next.flip

//         val idx =
//           TriangleMap.regularizeTriangleIndex(copy.vert, copy.next.vert, copy.next.next.vert)
//         _triangles += idx -> copy
//         copy
//       }

//       // TODO: Compute the distance from a point to an extent faster.
//       def circumcircleLeavesExtent(extent: Extent)(tri: Int): Boolean = {
//         val center =
//           circleCenter(getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri))))
//         val radius = center.distance2D(Coordinate(getX(getVert(tri)), getY(getVert(tri))))
//         val ppd = new PointPairDistance
//         val closest =
//           DistanceToPoint.computeDistance(extent.toPolygon.jtsGeom, center.toPoint.jtsGeom.getCoordinate, ppd)

//         ppd.getDistance < radius
//       }

//       def recursiveAddTris(e0: Int, opp0: HalfEdge[Int, Unit]): Unit = {
//         val workQueue = collection.mutable.Queue( (e0, opp0) )

//         while (!workQueue.isEmpty) {
//           val (e, opp) = workQueue.dequeue
//           val isOuterEdge = outerEdges.contains((getSrc(e), getVert(e)))
//           if (!isOuterEdge && opp.flip.face == None) {
//             lookupTriangle(e) match {
//               case Some(tri) => {
//                 // joinTriangles(opp, tri)
//                 opp.join(tri)
//               }
//               case None => {
//                 if (circumcircleLeavesExtent(boundingExtent)(e)) {
//                   val tri = copyConvertTriangle(e)
//                   // joinTriangles(opp, tri)
//                   opp.join(tri)
//                   workQueue.enqueue( (getFlip(getNext(e)), tri.next) )
//                   workQueue.enqueue( (getFlip(getNext(getNext(e))), tri.next.next) )
//                 }
//               }
//             }
//           }
//         }
//       }

//       def copyConvertBoundingTris(): HalfEdge[Int, Unit] = {
//         val newBound: HalfEdge[Int, Unit] =
//           copyConvertBoundingLoop(boundary, copyConvertEdge(boundary))
//         var e = boundary
//         var ne = newBound
//         val boundingTris = collection.mutable.Set.empty[Int]
//         do {
//           recursiveAddTris(getFlip(e), ne)
//           e = getNext(e)
//           ne = ne.next
//         } while (e != boundary)

//         assert(ne == newBound)
//         newBound
//       }

//       if (isLinear)
//         copyConvertLinearBound
//       else
//         copyConvertBoundingTris
//     }

//     BoundingMesh(boundingExtent, boundingEdge, _triangles.toMap, isLinear)
//   }
// }
