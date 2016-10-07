package geotrellis.osm

import geotrellis.util._
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

  // TODO
  def toGeometry(rdd: RDD[Node]): Feature[Geometry, TagMap] = {
    ???
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

class ElementToFeatureRDDMethods(val self: RDD[Element]) extends MethodExtensions[RDD[Element]] {
  def toFeatures: RDD[Feature[Geometry, TagMap]] = {

    val nodes: RDD[Node] = self.flatMap({
      case e: Node => Some(e)
      case _ => None
    })

    // Inefficient to do the flatMap twice!
    val ways: RDD[Way] = self.flatMap({
      case e: Node => None
      case e: Way  => Some(e)
    })

    /* TODO
     * 1. Convert all Ways to Lines and Polygons.
     * 2. Determine which Nodes were never used in a Way, and convert to Points.
     */

    ???
  }
}
