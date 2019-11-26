/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.store.compact

import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.effect.Sync

object FS2Utils {
  final class PartiallyAppliedFromIterator[F[_]](private val dummy: Boolean) extends AnyVal {
    def apply[A](iterator: Iterator[A])(implicit F: Sync[F]): fs2.Stream[F, A] = {
      def getNext(i: Iterator[A]): F[Option[(A, Iterator[A])]] =
        F.delay(i.hasNext).flatMap { b =>
          if (b) F.delay(i.next()).map(a => (a, i).some) else F.pure(None)
        }

      fs2.Stream.unfoldEval(iterator)(getNext)
    }
  }

  def fromIterator[F[_]]: PartiallyAppliedFromIterator[F] =
    new PartiallyAppliedFromIterator(dummy = true)
}
