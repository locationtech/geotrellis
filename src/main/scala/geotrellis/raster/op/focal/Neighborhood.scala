package geotrellis.raster.op.focal

import scala.math._

object Angles {
  @inline final def radians(d:Double) = d * Pi / 180.0
  @inline final def degrees(r:Double) = r * 180.0 / Pi
}

import Angles._

/**
 * Allows users of focal operations specify the shape and size
 * of the neighborhood the focal operation should use. 
 */
trait Neighborhood { 
  /* How many cells past the focus the bounding box goes (e.g., 1 for 9x9 square) */
  val extent:Int 
  /* Wether or not this neighborhood has a mask the cursor of it's bounding box */
  val hasMask:Boolean
  /* Overridden to define the mask of Neighborhoods that have one */
  def mask(x:Int,y:Int):Boolean = { false }
}

case class Square(extent:Int) extends Neighborhood {
  val hasMask = false
}

case class Circle(radius:Double) extends Neighborhood {
  val extent = ceil(radius).toInt
  val hasMask = true
  override def mask(x:Int,y:Int):Boolean = {
    val xx = x - extent
    val yy = y - extent
    sqrt(xx*xx+yy*yy) > radius
  }
}

/**
 * Simple neighborhood that goes through the center of the focus
 * along the x any y axis of the raster
 */
case class Nesw(extent:Int) extends Neighborhood {
  val hasMask = true
  override def mask(x:Int,y:Int) = { x != extent && y != extent  }
}

/**
 * Wedge neighborhood.
 *
 * @param     radius       The radius of the wedge, in raster cell units.
 * @param     startAngle   The starting angle of the wedge (in degrees).
 * @param     endAngle     The ending angle of the wedge (in degrees).
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

/**
 * Annulus neighborhood.
 *
 * @param     innerRadius   The radius of the inner circle of the Annulus.
 * @param     outerRadius   The radius of the outer circle of the Annulus.
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


