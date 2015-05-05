package geotrellis.spark.io.index.zcurve

import scala.annotation._ 

/**
 * Represents a rectangle in defined by min and max as two opposing points
 * 
 * @param min: lower-left point 
 * @param max: upper-right point
 */
case class Z2Range(min: Z2, max: Z2){  
  require(min.z < max.z || min.z == max.z, s"NOT: ${min.z} < ${max.z}")  
  def mid = min mid max

  def length = max.z - min.z
  
  def contains(z: Z2) = {
    val (x, y) = z.decode
    x >= min.dim(0) &&
    x <= max.dim(0) &&
    y >= min.dim(1) &&
    y <= max.dim(1)  
  }

  def contains(r: Z2Range): Boolean = 
    contains(r.min) && contains(r.max)

  def overlaps(r: Z2Range): Boolean = {
    def _overlaps(a1:Int, a2:Int, b1:Int, b2:Int) = math.max(a1,b1) <= math.min(a2,b2)
    _overlaps(min.dim(0), max.dim(0), r.min.dim(0), r.max.dim(0)) &&
    _overlaps(min.dim(1), max.dim(1), r.min.dim(1), r.max.dim(1))
  }
  
  /** 
   * Cuts Z-Range in two, can be used to perform augmented binary search
   * @parm xd: division point
   * @param inRange: is xd in query range
   */
  def cut(xd: Z2, inRange: Boolean): List[Z2Range] = {    
    if (min.z == max.z)
      Nil
    else if (inRange) {
      if (xd.z == min.z)      // degenerate case, two nodes min has already been counted
        Z2Range(max, max) :: Nil
      else if (xd.z == max.z) // degenerate case, two nodes max has already been counted
        Z2Range(min, min) :: Nil
      else
        Z2Range(min, xd-1) :: Z2Range(xd+1, max) :: Nil
    } else {      
      val (litmax, bigmin) = Z2.zdivide(xd, min, max)
      Z2Range(min, litmax) :: Z2Range(bigmin, max) :: Nil
    }
  }
}
