package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

object BoundingBox {
  def apply(xmin: Double, ymin: Double, xmax: Double, ymax: Double): BoundingBox =
    BoundingBox(new jts.Envelope(xmin, xmax, ymin, ymax))

  implicit def toPolygon(bb: BoundingBox): Polygon = 
    bb.toPolygon

  implicit def geom2BoundingBox(g: Geometry): BoundingBox =
    BoundingBox(g.jtsGeom.getEnvelopeInternal)

  implicit def jts2BoundingBox(env: jts.Envelope): BoundingBox =
    BoundingBox(env)
}

case class BoundingBox(jtsEnvelope: jts.Envelope) {
  lazy val xmin = jtsEnvelope.getMinX
  lazy val ymin = jtsEnvelope.getMinY
  lazy val xmax = jtsEnvelope.getMaxX
  lazy val ymax = jtsEnvelope.getMaxY

  lazy val min = Point(xmin, ymin)
  lazy val max = Point(xmax, ymax)

  /**
   * The SW corner (xmin, ymin) as a Point.
   */
  lazy val southWest = Point(xmin, ymin)

  /**
   * The SE corner (xmax, ymin) as a Point.
   */
  lazy val southEast = Point(xmax, ymin)
 
  /**
   * The NE corner (xmax, ymax) as a Point.
   */
  lazy val northEast = Point(xmax, ymax)

  /**
   * The NW corner (xmin, ymax) as a Point.
   */
  lazy val northWest = Point(xmin, ymax)

  lazy val width = jtsEnvelope.getWidth
  lazy val height = jtsEnvelope.getHeight

  lazy val area = jtsEnvelope.getArea
  lazy val minExtent = jtsEnvelope.minExtent
  lazy val maxExtent = jtsEnvelope.maxExtent

  lazy val center: Point = jtsEnvelope.centre

  def intersects(other: BoundingBox): Boolean =
    jtsEnvelope.intersects(other.jtsEnvelope)

  def intersects(p: Point): Boolean =
    intersects(p.x, p.y)

  def intersects(x: Double, y: Double): Boolean =
    jtsEnvelope.intersects(x, y)

  def contains(other: BoundingBox): Boolean =
    jtsEnvelope.contains(other.jtsEnvelope)

  def contains(p: Point): Boolean =
    contains(p.x, p.y)

  def contains(x: Double, y: Double): Boolean =
    jtsEnvelope.contains(x, y)

  def covers(other: BoundingBox): Boolean =
    jtsEnvelope.covers(other.jtsEnvelope)

  def covers(p: Point): Boolean =
    covers(p.x, p.y)

  def covers(x: Double, y: Double): Boolean =
    jtsEnvelope.covers(x, y)

  def distance(other: BoundingBox): Double =
    jtsEnvelope.distance(other.jtsEnvelope)

  def intersection(other: BoundingBox): Option[BoundingBox] = {
    val env = jtsEnvelope.intersection(other.jtsEnvelope)
    if(env.isNull) None
    else Some(BoundingBox(env))
  }

  /**
   * Orders two bounding boxes by their (geographically) lower-left corner. The bounding box
   * that is further south (or west in the case of a tie) comes first.
   *
   * If the lower-left corners are the same, the upper-right corners are
   * compared. This is mostly to assure that 0 is only returned when the
   * extents are equal.
   *
   * Return type signals:
   *
   *   -1 this bounding box comes first
   *    0 the bounding boxes have the same lower-left corner
   *    1 the other bounding box comes first
   */
  def compare(other: BoundingBox): Int = {
    var cmp = ymin compare other.ymin
    if (cmp != 0) return cmp

    cmp = xmin compare other.xmin
    if (cmp != 0) return cmp

    cmp = ymax compare other.ymax
    if (cmp != 0) return cmp

    xmax compare other.xmax
  }

  /** Create a copy and modify the envelope using JTS's mutable functions */
  private def modify(f: jts.Envelope => Unit): BoundingBox = {
    val bb = BoundingBox(xmin, ymin, xmax, ymax)
    f(bb.jtsEnvelope)
    bb
  }

  def expandToInclude(x: Double, y: Double) =
    modify(_.expandToInclude(x, y))

  def expandToInclude(p: Point): BoundingBox =
    expandToInclude(p.x, p.y)

  def expandToInclude(other: BoundingBox): BoundingBox =
    modify(_.expandToInclude(other.jtsEnvelope))

  def expandBy(deltaX: Double, deltaY: Double): BoundingBox =
    modify(_.expandBy(deltaX, deltaY))

  def expandBy(distance: Double): BoundingBox = 
    expandBy(distance, distance)

  def translate(deltaX: Double, deltaY: Double): BoundingBox =
    modify(_.translate(deltaX, deltaY))

  def toPolygon(): Polygon = 
    Polygon( Line((xmin, ymin), (xmin, ymax), (xmax, ymax), (xmax, ymin), (xmin, ymin)) )

  override def toString: String =
    s"BBOX[$xmin, $ymin, $xmax, $ymax]"
}
