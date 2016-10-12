package geotrellis.osm

import geotrellis.util._
import geotrellis.vector._

import org.apache.spark.rdd._

// --- //

class ElementToFeatureRDDMethods(val self: RDD[Element]) extends MethodExtensions[RDD[Element]] {
  def toFeatures: RDD[Feature[Geometry, TagMap]] = {

    /* All OSM nodes, indexed by their Element id */
    val nodes: RDD[(Long, Node)] = self.flatMap({
      case e: Node => Some(e)
      case _ => None
    }).map(n => (n.meta.id, n))

    // Inefficient to do the flatMap twice!
    val ways: RDD[Way] = self.flatMap({
      case e: Node => None
      case e: Way  => Some(e)
    })

    /* TODO
     * 1. Convert all Ways to Lines and Polygons.
     * 2. Determine which Nodes were never used in a Way, and convert to Points.
     */

    /* You're a long way from finishing this operation. */
    val links: RDD[(Long, Way)] = ways.flatMap(w => w.nodes.map(n => (n, w)))

    val grouped: RDD[(Long, (Iterable[Node], Iterable[Way]))] = nodes.cogroup(links)

    val linesPolys: RDD[Feature[Geometry, TagMap]] =
      grouped
        .flatMap({ case (_, (ns, ws)) =>
          val n = ns.head

          ws.map(w => (w, n))
        })
        .groupByKey
        .map({ case (w, ns) =>
          /* De facto maximum of 2000 Nodes */
          val sorted: Vector[Node] = ns.toVector.sortBy(n => n.meta.id)

          /* `get` is safe, the BTree is guaranteed to be populated */
          val tree: BTree[Node] = BTree.fromSortedSeq(sorted).get

          /* A binary search branch predicate */
          val pred: (Long, BTree[Node]) => Either[Option[BTree[Node]], Node] = { (n, tree) =>
            if (n == tree.value.meta.id) {
              Right(tree.value)
            } else if (n < tree.value.meta.id) {
              Left(tree.left)
            } else {
              Left(tree.right)
            }
          }

          /* The actual node coordinates in the correct order */
          val points: Vector[(Double, Double)] =
            w.nodes
              .flatMap(n => tree.searchWith(n, pred))
              .map(n => (n.lat, n.lon))

          // TODO Holed Polygons aren't handled yet.
          val g: Geometry = if (w.isLine) Line(points) else Polygon(points)

          Feature(g, w.tagMap)
        })

    /* Single Nodes unused in any Way */
    val points: RDD[Feature[Geometry, TagMap]] = grouped.flatMap({ case (_, (ns, ws)) =>
      if (ws.isEmpty) {
        val n = ns.head

        Some(Feature(Point(n.lat, n.lon), n.tagMap))
      } else {
        None
      }
    })

    linesPolys ++ points
  }
}
