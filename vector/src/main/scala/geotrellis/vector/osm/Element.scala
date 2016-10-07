package geotrellis.vector.osm

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
  */
case class Way(
  nodes: Seq[Long],
  meta: ElementMeta,
  tagMap: TagMap
) extends Element

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
