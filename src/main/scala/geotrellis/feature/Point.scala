package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }

class Point[D] (override val geom:jts.Point, data:D) extends SingleGeometry(geom,data)

/**
 * Point represents a simple (x,y) coordinate.
 *
 *
 */
object Point {
  def factory = Feature.factory

  /**
   * Create a point feature.
   *
   * @param   x   x coordinate
   * @param   y   y coordinate
   * @param   d   Data of this feature
   */
  def apply[D](x: Double, y: Double, data: D) = {
    val p = factory.createPoint(new jts.Coordinate(x, y))
    JtsPoint(p, data)
  }

  /**
   * Create a point feature without data.
   *
   * @param   x   x coordinate
   * @param   y   y coordinate
   */
  def apply(x: Double, y: Double) = {
    JtsPoint(factory.createPoint(new jts.Coordinate(x, y)), None)
  } 
  /**
   * Create a point feature from a JTS point instance.
   *
   * @param p     JTS Point instance
   * @param data  Data of this feature.
   */
  def apply[D](p: jts.Point, data: D) = new JtsPoint(p, data)

  /**
   * Calculate row and column of this point in given raster extent.
   *
   * @param p             Point feature
   * @param rasterExtent  Raster extent to place feature within
   */
  def pointToGridCoords(p: Point[_], rasterExtent: RasterExtent) = {
    val geom = p.geom
    rasterExtent.mapToGrid(geom.getX(), geom.getY())
  }

  /**
   * Get value from raster at cell corresponding to given point.
   *
   * @param p       Point feature
   * @param raster  Raster to get value from
   */
  def pointToRasterValue(p: Feature[jts.Point, _], raster: Raster) = {
    val re = raster.rasterExtent
    raster.get(re.mapXToGrid(p.geom.getX()), re.mapYToGrid(p.geom.getY()))
  }
}

/**
 * Point feature with a JTS Point internal representation.
 */
case class JtsPoint[D](g: jts.Point, d: D) extends Point(g,d)
