package geotrellis

import scala.annotation.tailrec

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
  def reducePairwise[A](as: List[A])(f: (A, A) => A): Option[A] = as match {
    case Nil      => None
    case a :: Nil => Some(a)
    case as       => reducePairwise(pairwise(as, Nil)(f))(f)
  }

  /**
    * This function uses an associative binary function "f" to combine
    * elements of a List[A] pairwise into a shorter List[A].
    *
    * For instance, List(1,2,3,4,5) results in List(5, f(3, 4), f(1, 2)).
    */
  @tailrec
  def pairwise[A](as: List[A], sofar: List[A])(f: (A, A) => A): List[A] = {
    as match {
      case a1 :: a2 :: as => pairwise(as, f(a1, a2) :: sofar)(f)
      case a :: Nil       => a :: sofar
      case Nil            => sofar
    }
  }
}
