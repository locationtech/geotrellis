package geotrellis.spark


import monocle.Lens

import scalaz.Functor

trait SerializableLens[S, T, A, B] extends Lens[S, T, A, B] with Serializable

object SerializableLens {
  def apply[S, T, A, B](_get: S => A, _set: (S, B) => T): SerializableLens[S, T, A, B] = new SerializableLens[S, T, A, B]  with Serializable{
    def lift[F[_]: Functor](from: S, f: A => F[B]): F[T] =
      Functor[F].map(f(_get(from)))(newValue => _set(from, newValue))
  }
}

object KeyLens {
  def apply[S, A](_get: S => A, _set: (S, A) => S): KeyLens[S, A] = SerializableLens[S, S, A, A](_get, _set)
}
