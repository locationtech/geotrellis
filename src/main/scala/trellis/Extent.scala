package trellis

import scala.math.{min,max}


case class ExtentRangeError(msg:String) extends Exception(msg)

/**
 * An Extent represents a rectangular region of geographic space (with a
 * particular projection). It is expressed in map coordinates. It is not
 * concerned with details of e.g. cell sizes (for that, see GeoAttrs).
 */
case class Extent(xmin:Double, ymin:Double, xmax:Double, ymax:Double) {
  if (xmin > xmax) throw ExtentRangeError("x: %s to %s".format(xmin, xmax))
  if (ymin > ymax) throw ExtentRangeError("y: %s to %s".format(ymin, ymax))
  
  val height = ymax - ymin
  val width = xmax - xmin
  
  /**
   * Orders two extents by their (geographically) lower-left corner. The extent
   * that is further south (or west in the case of a tie) comes first.
   *
   * Return type signals:
   *
   *   -1 this extent comes first
   *    0 the extents have the same lower-left corner
   *    1 the other extent comes first
   */
  def compare(other:Extent):Int = {
    val cmp = ymin.compare(other.ymin)
    if (cmp != 0) cmp else xmin.compare(other.xmin)
  }
  
  /**
   * Return a the smallest extent that contains this extent and the provided
   * extent. This is provides a union of the two extents.
   */
  def combine(other:Extent) = {
    val xminNew = min(xmin, other.xmin)
    val xmaxNew = max(xmax, other.xmax)
    val yminNew = min(ymin, other.ymin)
    val ymaxNew = max(ymax, other.ymax)
    Extent(xminNew, yminNew, yminNew, ymaxNew)
  }
  
  /**
   * Determine whether the given point lies within the extent. The boundary
   * is excluded, so that Extent(a, b, c, d) does not contain (a, b).
   */
  def containsPoint(x:Double, y:Double) = {
    x > xmin && x < xmax && y > ymin && y < ymax
  }
  
  /**
   * Return SW corner (xmin, ymin) as tuple.
   */
  def southWest() = { (xmin, ymin) }
  
  /**
   * Return NE corner (xmax, ymax) as tuple.
   */
  def northEast() = { (xmax, ymax) }
}
