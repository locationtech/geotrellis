package geotrellis.proj4
import scala.collection.mutable
/**
 * @author Manuri Perera
 */
class Memoize[T, R](f: T => R,map:mutable.Map[T,R]) extends (T => R) {
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

