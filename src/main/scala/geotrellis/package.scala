import scala.annotation.tailrec

//import annotation.implicitNotFound

package object geotrellis {
  // TOOD: phase out geotrellis.contants package
  final val NODATA = Int.MinValue

  type Op[+A] = Operation[A]
  //type DispatchedOp[+T] = DispatchedOperation[T]

  //@implicitNotFound(msg = "Cannot find NoData type class for ${T}")
  //trait NoData[T] {
  //  def value:T
  //}

  //implicit object IntNoData extends NoData[Int] { final def value = Int.MinValue }
  //implicit object LongNoData extends NoData[Long] { final def value = Long.MinValue }
  //implicit object FloatNoData extends NoData[Float] { final def value = Float.NaN }
  //implicit object DoubleNoData extends NoData[Double] { final def value = Double.NaN }
}

package geotrellis {
  package object process {
    type Callback[T] = (List[Any]) => StepOutput[T]
    type Args = List[Any]
  
    def time() = System.currentTimeMillis
    def log(msg:String) = if (false) println(msg)
  }

  package object util {
    /**
     * This function uses an associative binary function "f" to combine the
     * elements of a List[A] into a Option[A].
     * 
     * If the list is empty, None is returned.
     * If the list is non-empty, Some[A] will be returned.
     *
     * For example, List(1,2,3,4)(f) results in Some(f(f(3, 4), f(1, 2))).
     */
    @tailrec
    def reducePairwise[A](as:List[A])(f:(A, A) => A):Option[A] = as match {
      case Nil => None
      case a :: Nil => Some(a)
      case as => reducePairwise(pairwise(as, Nil)(f))(f)
    }
  
    /**
     * This function uses an associative binary function "f" to combine
     * elements of a List[A] pairwise into a shorter List[A].
     *
     * For instance, List(1,2,3,4,5) results in List(5, f(3, 4), f(1, 2)).
     */
    @tailrec
    def pairwise[A](as:List[A], sofar:List[A])(f:(A, A) => A):List[A] = {
      as match {
        case a1 :: a2 :: as => pairwise(as, f(a1, a2) :: sofar)(f)
        case a :: Nil => a :: sofar
        case Nil => sofar
      }
    }
  }
}
