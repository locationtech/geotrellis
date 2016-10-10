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

    nodes
      .cogroup(links)
      .flatMap({ case (_, (ns, ws)) =>
        val n = ns.head

        ws.map(w => (w, (n.lat, n.lon)))
      })
      .groupByKey
      .map({ case (w, ns) =>
        val line = Line(ns)

        // TODO Holed Polygons aren't handled yet.
        val g: Geometry = if (w.isLine) line else Polygon(line)

        Feature(g, w.tagMap)
      })
  }
}
