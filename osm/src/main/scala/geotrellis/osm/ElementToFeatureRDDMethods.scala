package geotrellis.osm

import geotrellis.util._
import geotrellis.vector._

import org.apache.spark.rdd._

// --- //

class ElementToFeatureRDDMethods(val self: RDD[Element]) extends MethodExtensions[RDD[Element]] {
  def toFeatures: RDD[OSMFeature] = {

    /* All OSM Nodes */
    val nodes: RDD[Node] = self.flatMap({
      case e: Node => Some(e)
      case _ => None
    })

    /* All OSM Ways */
    val ways: RDD[Way] = self.flatMap({
      case e: Way  => Some(e)
      case _ => None
    })

    // Inefficient to do the flatMap thrice!
    // A function `split: RDD[Element] => (RDD[Node], RDD[Way], RDD[Relation])
    // would be nice.
    /* All OSM Relations */
    val relations: RDD[Relation] = self.flatMap({
      case e: Relation => Some(e)
      case _ => None
    }).filter({ r => /* Baby steps. Limit to handling multipolys for now */
      r.tagMap.get("type") match {
        case Some("multipolygon") => true
        case _ => false
      }
    })

    val (points, lines, polys) = geometries(nodes, ways)

    val finalPolys: RDD[OSMFeature] =
      multipolygons(polys, relations).asInstanceOf[RDD[OSMFeature]]

    points.asInstanceOf[RDD[OSMFeature]] ++ lines.asInstanceOf[RDD[OSMFeature]] ++ finalPolys
  }

  private def multipolygons(
    polys: RDD[Feature[Polygon, TagMap]],
    relations: RDD[Relation]
  ): RDD[Feature[Polygon, TagMap]] = {
    polys  // TODO: From here.
  }

  /** Every OSM Node and Way converted to GeoTrellis Geometries.
    * This includes Points, Lines, and Polygons which have no holes.
    * Holed polygons are handled by [[multipolygons]], as they are represented
    * by OSM Relations.
    */
  private def geometries(
    nodes: RDD[Node],
    ways: RDD[Way]
  ): (RDD[Feature[Point, TagMap]], RDD[Feature[Line, TagMap]], RDD[Feature[Polygon, TagMap]]) = {
    /* You're a long way from finishing this operation. */
    val links: RDD[(Long, Way)] = ways.flatMap(w => w.nodes.map(n => (n, w)))

    val grouped: RDD[(Long, (Iterable[Node], Iterable[Way]))] =
      nodes.map(n => (n.meta.id, n)).cogroup(links)

    val linesPolys: RDD[Either[Feature[Line, TagMap], Feature[Polygon, TagMap]]] =
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


          if (w.isLine) {
            Left(Feature(Line(points), w.tagMap))
          } else {
            Right(Feature(Polygon(points), w.tagMap))
          }
        })

    // TODO: More inefficient RDD splitting.
    val lines: RDD[Feature[Line, TagMap]] = linesPolys.flatMap({
      case Left(l) => Some(l)
      case _ => None
    })

    val polys: RDD[Feature[Polygon, TagMap]] = linesPolys.flatMap({
      case Right(p) => Some(p)
      case _ => None
    })

    /* Single Nodes unused in any Way */
    val points: RDD[Feature[Point, TagMap]] = grouped.flatMap({ case (_, (ns, ws)) =>
      if (ws.isEmpty) {
        val n = ns.head

        Some(Feature(Point(n.lat, n.lon), n.tagMap))
      } else {
        None
      }
    })

    (points, lines, polys)
  }
}
