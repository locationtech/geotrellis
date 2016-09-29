package geotrellis.util

// --- //

/** A Functor definition that exposes its initial inner type.
  * This allows us to impose additional (implicit) constraints
  * on it, for instance a `SpatialComponent` that might be
  * required on `A` within the [[map]] function.
  */
trait Functor[F[_], A] {

  /** Lift `f` into `F` and apply to `F[A]`. */
  def map[B](fa: F[A])(f: A => B): F[B]

  def apply[B](fa: F[A])(f: A => B): F[B] = map(fa)(f)
}
