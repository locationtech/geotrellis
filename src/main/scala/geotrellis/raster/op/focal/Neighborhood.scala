package geotrellis.raster.op.focal

import scala.math._

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

case class Square2(extent:Int) extends Neighborhood {
  val hasMask = false
}

case class Circle2(radius:Double) extends Neighborhood {
  val extent = ceil(radius).toInt
  val hasMask = true
  override def mask(x:Int,y:Int):Boolean = {
    val xx = x - extent
    val yy = y - extent
    sqrt(xx*xx+yy*yy) > radius
  }
}


sealed trait NeighborhoodType

/** Defines a neighborhood consisting of only horizontal and vertical neighbors */
case class Nesw() extends NeighborhoodType

sealed trait ExtendableNeighborhoodType { 
  /** A one dimensional description of the size of the neighborhood */
  val extent:Int 
}

/** Defines a square neighborhood */
case class Square(extent: Int) extends NeighborhoodType
/** Defines a circular neighborhood */
case class Circle(extent: Int) extends NeighborhoodType

