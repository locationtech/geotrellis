package geotrellis.osm

import geotrellis.vector._

import org.apache.spark.rdd._
import java.time.ZonedDateTime

// --- //

/** A sum type for OSM Elements. All Element types share some common attributes. */
sealed trait Element {
  def meta: ElementMeta
  def tagMap: TagMap
}

/** Some point in the world, which could represent a location or small object
  * like a park bench or flagpole.
  */
case class Node(
  lat: Double,
  lon: Double,
  meta: ElementMeta,
  tagMap: TagMap
) extends Element

/** A string of [[Node]]s which could represent a road, or if connected back around
  * to itself, a building, water body, landmass, etc.
  *
  * Assumption: A Way has at least two nodes.
  */
case class Way(
  nodes: Vector[Long],
  meta: ElementMeta,
  tagMap: TagMap
) extends Element {
  /** Is it a Polyline, but not an "Area" even if closed? */
  def isLine: Boolean = !isClosed || (!isArea && isHighwayOrBarrier)

  def isClosed: Boolean = if (nodes.isEmpty) false else nodes(0) == nodes.last

  def isArea: Boolean = tagMap.get("area").map(_ == "yes").getOrElse(false)

  def isHighwayOrBarrier: Boolean = {
    val tags: Set[String] = tagMap.keySet

    tags.contains("highway") || tags.contains("barrier")
  }

  /** ASSUMPTION: This `Way` is not degenerate, therefore:
    *   1. has at least two Nodes
    *   2. only references Nodes which exist
    */
  def toGeometry(rdd: RDD[(Long, Node)]): Feature[Geometry, TagMap] = {
    // TODO Potential for Ramer–Douglas–Peucker algorithm here
    // for some data reduction.
    val ns: Vector[Node] = nodes.map(n => rdd.lookup(n).head)

    val line = Line(ns.map(node => (node.lat, node.lon)))  // TODO LatLon is correct?

    // TODO Holed Polygons aren't handled yet.
    val g: Geometry = if (isLine) line else Polygon(line)

    Feature(g, tagMap)
  }
}

/** All Element types have these attributes in common. */
case class ElementMeta(
  id: Long,
  user: String,
  userId: String,
  changeSet: Int,
  version: Int,
  timestamp: ZonedDateTime,
  visible: Boolean
)
