package geotrellis.util

/**
  * Utility class for timing the execution time of a function.
  */
object Timer {
  def time[T](thunk: => T) = {
    val t0 = System.currentTimeMillis()
    val result = thunk
    val t1 = System.currentTimeMillis()
    (result, t1 - t0)
  }

  def run[T](thunk: => T) = {
    val (result, t) = time { thunk }
    printf("%s: %d ms\n", result, t)
    result
  }

  def log[T](fmt:String, args:Any*)(thunk: => T) = {
    val label = fmt.format(args:_*)
    val (result, t) = time { thunk }
    printf(label + ": %d ms\n".format(t))
    result
  }
}
