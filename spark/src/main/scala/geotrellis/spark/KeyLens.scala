package geotrellis.spark

import geotrellis.spark.utils._

import monocle._

import scalaz.Functor

class SerializableLens[S, T, A, B](_get: S => A, _set: (B => S => T)) extends Serializable {

  def lens: PLens[S, T, A, B] = PLens(_get)(_set)

  def lift[F[_]: Functor](from: S, f: A => F[B]): F[T] = Functor[F]
    .map(f(_get(from)))(newValue => _set(newValue)(from))

}

object SerializableLens {
  def apply[S, T, A, B](
    _get: S => A,
    _set: (B => S => T)): SerializableLens[S, T, A, B] = new SerializableLens(_get, _set)
}

object KeyLens {
  def apply[S, A](
    _get: S => A,
    _set: (A => S => S)): KeyLens[S, A] = SerializableLens[S, S, A, A](_get, _set)
}
