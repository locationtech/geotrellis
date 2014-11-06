package geotrellis.spark.utils

class KryoClosure1[A, B](f: A=>B) extends KryoWrapper[A=>B] with Function1[A, B] with Serializable {
  assert(f != null)
  value = f

  def apply(in: A): B = {
    assert(value != null)
    value.apply(in)
  }
}
class KryoClosure2[T1, T2, R](f: (T1,T2)=>R) extends KryoWrapper[(T1, T2)=>R] with Function2[T1, T2, R] with Serializable {
  assert(f != null)
  value = f

  def apply(t1: T1, t2: T2): R = {
    assert(value != null)
    value.apply(t1,t2)
  }
}

object KryoClosure {
  import com.esotericsoftware.minlog.Log
  import com.esotericsoftware.minlog.Log._
  Log.set(LEVEL_TRACE)

  def apply[A, B](f: A => B)  = new KryoClosure1[A, B](f)
  def apply[T1, T2, R](f: (T1,T2) => R)  = new KryoClosure2[T1, T2, R](f)
}
