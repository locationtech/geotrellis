package geotrellis

import scala.math.{min,max}

import geotrellis.feature.Polygon

case class ExtentRangeError(msg:String) extends Exception(msg)

/**
 * An Extent represents a rectangular region of geographic space (with a
 * particular projection). It is expressed in map coordinates. It is not
 * concerned with details of e.g. cell sizes (for that, see RasterExtent).
 */
case class Extent(xmin:Double, ymin:Double, xmax:Double, ymax:Double) {
  if (xmin > xmax) throw ExtentRangeError(s"x: $xmin to $xmax")
  if (ymin > ymax) throw ExtentRangeError(s"y: $ymin to $ymax")
  
  val height = ymax - ymin
  val width = xmax - xmin
  
  /**
   * Orders two extents by their (geographically) lower-left corner. The extent
   * that is further south (or west in the case of a tie) comes first.
   *
   * If the lower-left corners are the same, the upper-right corners are
   * compared. This is mostly to assure that 0 is only returned when the
   * extents are equal.
   *
   * Return type signals:
   *
   *   -1 this extent comes first
   *    0 the extents have the same lower-left corner
   *    1 the other extent comes first
   */
  def compare(other:Extent):Int = {
    var cmp = ymin compare other.ymin
    if (cmp != 0) return cmp

    cmp = xmin compare other.xmin
    if (cmp != 0) return cmp

    cmp = ymax compare other.ymax
    if (cmp != 0) return cmp

    xmax compare other.xmax
  }
  
  /**
   * Return a the smallest extent that contains this extent and the provided
   * extent. This is provides a union of the two extents.
   */
  def combine(other:Extent):Extent = {
    val xminNew = min(xmin, other.xmin)
    val xmaxNew = max(xmax, other.xmax)
    val yminNew = min(ymin, other.ymin)
    val ymaxNew = max(ymax, other.ymax)
    Extent(xminNew, yminNew, xmaxNew, ymaxNew)
  }

  /**
   * Takes the intersection of two extents.
   */
  def intersect(other:Extent):Option[Extent] = {
    val xminNew = max(xmin, other.xmin)
    val xmaxNew = min(xmax, other.xmax)
    val yminNew = max(ymin, other.ymin)
    val ymaxNew = min(ymax, other.ymax)
    if(xminNew <= xmaxNew && yminNew <= ymaxNew) {
      Some(Extent(xminNew, yminNew, xmaxNew, ymaxNew))
    } else { None }
  }
  
  /**
   * Determine whether the given point lies within the extent. The boundary
   * is excluded, so that Extent(a, b, c, d) does not contain (a, b).
   */
  def containsPoint(x:Double, y:Double) = {
    x > xmin && x < xmax && y > ymin && y < ymax
  }

  /**
   * Determines whether the given extent is contained within the extent.
   */
  def containsExtent(other:Extent) = {
    other.xmin >= xmin &&
    other.ymin >= ymin &&
    other.xmax <= xmax &&
    other.ymax <= ymax
  }
  
  /**
   * Return SW corner (xmin, ymin) as tuple.
   */
  def southWest = (xmin, ymin)

  /**
   * Return SE corner (xmax, ymin) as tuple.
   */
  def southEast = (xmax, ymin)
 
  /**
   * Return NE corner (xmax, ymax) as tuple.
   */
  def northEast = (xmax, ymax)

  /**
   * Return NW corner (xmin, ymax) as tuple.
   */
  def northWest = (xmin, ymax)

  /**
   * Return extent as feature.
   */
  def asFeature[D](data:D) = Polygon(
    List(northEast, southEast, southWest, northWest, northEast),
    data
  )
}
