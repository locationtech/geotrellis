package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import scala.math._
import scala.language.implicitConversions

object Angles {
  @inline final def radians(d:Double) = d * Pi / 180.0
  @inline final def degrees(r:Double) = r * 180.0 / Pi
}

import Angles._

/**
 * A definition of the shape and size
 * of the neighborhood (or kernel) to be used in a focal operation.
 */
trait Neighborhood { 
  /** How many cells past the focus the bounding box goes. (e.g., 1 for 9x9 square) */
  val extent:Int 

  /** Whether or not this neighborhood has a mask the cursor of it's bounding box */
  val hasMask:Boolean

  /** Overridden to define the mask of Neighborhoods that have one 
   *  col and row treat the mask as a raster, that is, (0,0) is the
   *  upper left corner, and (extent*2+1,extent*2+1) is the lower
   *  right corner
   */
  def mask(col:Int,row:Int):Boolean = { false }

  override
  def toString = {
    val sb = new scala.collection.mutable.StringBuilder()
    val d = extent*2 + 1
    for(y <- 0 until d) {
      for(x <- 0 until d) {
        if(mask(x,y)) { sb.append(" X ") }
        else { sb.append(" O ") }
      }
      sb.append("\n")
    }
    sb.toString
  }
}

/** A square neighborhood.
 *
 * @param   extent   Extent of the neighborhood.
 *                   The extent is how many cells past the focus the bounding box goes.
 *                   (e.g., 1 for 3x3 square)
 */
case class Square(extent:Int) extends Neighborhood {
  val hasMask = false
}

/** A circle neighborhood.
 *
 * @param   radius   Radius of the circle that defines which cells inside the bounding
 *                   box will be considered part of the neighborhood.
 *
 * @note Cells who's distance from the center is exactly the radius '''are''' included
 *       in the neighborhood.
 */
case class Circle(radius:Double) extends Neighborhood {
  val extent = ceil(radius).toInt
  val hasMask = true
  override def mask(x:Int,y:Int):Boolean = {
    val xx = x - extent
    val yy = y - extent
    sqrt(xx*xx+yy*yy) > radius
  }
}

/** A neighborhood that includes a column and row intersectin the focus.
 *
 * @param   extent   Extent of the neighborhood.
 *                   The extent is how many cells past the focus the bounding box goes.
 *                   (e.g., 1 for 9x9 square)
 */
case class Nesw(extent:Int) extends Neighborhood {
  val hasMask = true
  override def mask(x:Int,y:Int) = { x != extent && y != extent  }
}

/** Wedge neighborhood.
 *
 * @param     radius       The radius of the wedge, in raster cell units.
 * @param     startAngle   The starting angle of the wedge (in degrees).
 * @param     endAngle     The ending angle of the wedge (in degrees).
 *
 * @note Cells who's distance from the center is exactly the radius '''are''' included
 *       in the neighborhood.
 */
case class Wedge(radius:Double,startAngle:Double,endAngle:Double) extends Neighborhood {
  val extent = ceil(radius).toInt
  val startRad = radians(startAngle)
  val endRad = radians(endAngle)

  val isInAngle = if(startRad > endRad) {
                    (rads:Double) => startRad <= rads || rads <= endRad
                  } else {
                    (rads:Double) => startRad <= rads && rads <= endRad
                  }

  val hasMask = true
  override def mask(x:Int,y:Int) = {
    val xx = x - extent
    val yy = extent - y

    var angle = atan2(yy,xx)
    if(angle < 0 ) angle += 2*Pi

    (sqrt(xx*xx+yy*yy) > radius || !isInAngle(angle)) &&
     !(xx == 0 && yy == 0)
  }
}

/** Annulus neighborhood.
 *
 * @param     innerRadius   The radius of the inner circle of the Annulus.
 * @param     outerRadius   The radius of the outer circle of the Annulus.
 *
 * @note Cells who's distance from the center is exactly the inner or outer radius
 *       '''are''' included in the neighborhood.
 */
case class Annulus(innerRadius:Double,outerRadius:Double) extends Neighborhood {
  val extent = ceil(outerRadius).toInt

  val hasMask = true
  override def mask(x:Int,y:Int) = {
    val xx = x - extent
    val yy = y - extent
    val len = sqrt(xx*xx+yy*yy)
    len < innerRadius || outerRadius < len
  }
}
