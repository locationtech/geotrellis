package trellis

import Predef.{any2stringadd => _, _}
import scala.{specialized => spec}
import com.azavea.math.Numeric
import com.azavea.math.FastImplicits._

/**
 * GenRasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop in replacement for Array.
 *
 * Currently only Int, instead of generic.
 */
trait GenRasterData[@spec T] {
  implicit val n:Numeric[T]

  def apply(i:Int):T
  def copy():GenRasterData[T]
  def length:Int
  def update(i:Int, x:T)
  def asArray:Array[T]
  def asList = asArray.toList

  override def toString = "GenRasterData(<%d values>)" format length

  override def equals(other:Any):Boolean = other match {
    case r:GenRasterData[_] => {
      if (r == null) return false
      if (length != r.length) return false
      var i = 0
      val len = length
      while (i < len) {
        if (apply(i) != r(i)) return false
        i += 1
      }
      true
    }
    case _ => false
  }
}

class ArrayGenRasterData[@spec T:Numeric:Manifest](array:Array[T]) extends GenRasterData[T] with Serializable {
  val n = implicitly[Numeric[T]]

  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, x:T):Unit = array(i) = x
  def copy = ArrayGenRasterData(array.clone)
  def asArray = array
}

object ArrayGenRasterData {
  def apply[@spec T:Numeric:Manifest](array:Array[T]) = new ArrayGenRasterData(array)
}
