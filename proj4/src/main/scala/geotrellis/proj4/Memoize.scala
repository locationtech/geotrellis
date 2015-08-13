package geotrellis.proj4

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Manuri Perera
 */
class Memoize[T, R](f: T => R) extends (T => R) {
  val map: ConcurrentHashMap[T, R] = new ConcurrentHashMap()

  def apply(x: T): R = {
    if (map.contains(x)) map.get(x)
    else {
      val y = f(x)
      map.put(x, y)
      y
    }
  }
}

