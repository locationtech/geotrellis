/*
 * Copyright 2018 Azavea
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

package geotrellis.store

import cats.effect.Async
import cats.syntax.functor._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Statement}

import java.math.BigInteger
import java.util.concurrent.CompletableFuture

package object cassandra extends Serializable {
  implicit def bigToBig(i: BigInt): BigInteger = new BigInteger(i.toByteArray)

  implicit class CompletableFutureOps[T](val self: CompletableFuture[T]) extends AnyVal {
    def liftTo[F[_]: Async]: F[T] = Async[F].fromCompletableFuture(Async[F].delay(self))
  }

  implicit class CqlSessionOps(val self: CqlSession) extends AnyVal {
    def closeF[F[_]: Async]: F[Unit] = self.closeAsync().toCompletableFuture.liftTo.void
    def executeF[F[_]: Async](statement: Statement[_]): F[AsyncResultSet] = self.executeAsync(statement).toCompletableFuture.liftTo
  }

  implicit class AsyncResultSetOps(val self: AsyncResultSet) extends AnyVal {
    def nonEmpty: Boolean = self.remaining() > 0 || self.hasMorePages
    def isEmpty: Boolean = !nonEmpty
  }
}
