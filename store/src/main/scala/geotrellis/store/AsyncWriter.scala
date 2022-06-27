/*
 * Copyright 2016 Azavea
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

import geotrellis.store.util.IORuntimeTransient
import geotrellis.store.util.IOUtils._

import cats.effect._
import cats.syntax.either._

import scala.util.{Failure, Success, Try}

abstract class AsyncWriter[Client, V, E](runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime) extends Serializable {

  def readRecord(client: Client, key: String): Try[V]

  def encodeRecord(key: String, value: V): E

  def writeRecord(client: Client, key: String, encoded: E): Try[Long]

  def write(
    client: Client,
    partition: Iterator[(String, V)],
    mergeFunc: Option[(V, V) => V] = None,
    retryFunc: Option[Throwable => Boolean]
  ): Unit = {
    if (partition.nonEmpty) {
      implicit val ioRuntime: unsafe.IORuntime = runtime

      val rows: fs2.Stream[IO, (String, V)] = fs2.Stream.fromIterator[IO](partition, chunkSize = 1)

      def elaborateRow(row: (String, V)): fs2.Stream[IO, (String, V)] = {
        val foldUpdate: ((String, V)) => (String, V) = {
          case newRecord@(key, newValue) =>
            mergeFunc match {
              case Some(fn) =>
                // TODO: match on this failure to retry reads
                readRecord(client, key) match {
                  case Success(oldValue) => (key, fn(newValue, oldValue))
                  case Failure(_) => newRecord
                }
              case None => newRecord
            }
        }

        fs2.Stream eval IO.blocking(foldUpdate(row))
      }

      def encode(row: (String, V)): fs2.Stream[IO, (String, E)] = {
        val (key, value) = row
        val encodeTask = IO.blocking((key, encodeRecord(key, value)))
        fs2.Stream eval encodeTask
      }

      def retire(row: (String, E)): fs2.Stream[IO, Try[Long]] = {
        val writeTask = IO.blocking(writeRecord(client, row._1, row._2))
        fs2.Stream eval retryFunc.fold(writeTask)(writeTask.retryEBO(_))
      }

      rows
        .flatMap(elaborateRow)
        .flatMap(encode)
        .map(retire)
        .parJoinUnbounded
        .compile
        .toVector
        .attempt
        .unsafeRunSync()
        .valueOr(throw _)
    }
  }
}
