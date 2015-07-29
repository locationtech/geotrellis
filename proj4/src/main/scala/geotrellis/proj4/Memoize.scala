package geotrellis.proj4
import scala.collection.mutable
/**
 * @author Manuri Perera
 */
class Memoize[T, R](f: T => R) extends (T => R) {
  val map: mutable.Map[T,R] = mutable.Map.empty[T,R]
  def apply(x: T): R = {
    if (map.contains(x)) {
      map(x)
    }
    else {
      val y = f(x)
      map += ((x, y))
      y
    }
  }
}

