package geotrellis.osm

import java.time.ZonedDateTime

// --- //

/** A sum type for OSM Elements. All Element types share some common attributes. */
sealed trait Element {
  def data: ElementData
}

/** Some point in the world, which could represent a location or small object
  * like a park bench or flagpole.
  */
case class Node(
  lat: Double,
  lon: Double,
  data: ElementData
) extends Element

/** A string of [[Node]]s which could represent a road, or if connected back around
  * to itself, a building, water body, landmass, etc.
  *
  * Assumption: A Way has at least two nodes.
  */
case class Way(
  nodes: Vector[Long],  /* Vector for O(1) indexing */
  data: ElementData
) extends Element {
  /** Is it a Polyline, but not an "Area" even if closed? */
  def isLine: Boolean = !isClosed || (!isArea && isHighwayOrBarrier)

  def isClosed: Boolean = if (nodes.isEmpty) false else nodes(0) == nodes.last

  def isArea: Boolean = data.tagMap.get("area").map(_ == "yes").getOrElse(false)

  def isHighwayOrBarrier: Boolean = {
    val tags: Set[String] = data.tagMap.keySet

    tags.contains("highway") || tags.contains("barrier")
  }
}

case class Relation(
  members: Seq[Member],
  data: ElementData
) extends Element

case class Member(
  memType: String, // TODO Use a sum type?
  ref: Long,
  role: String // TODO Use a sum type?
)

case class ElementData(meta: ElementMeta, tagMap: TagMap)

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
