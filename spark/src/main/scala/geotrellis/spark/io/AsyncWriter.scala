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

package geotrellis.spark.io

import java.util.concurrent.Executors

import scala.util.{Failure, Success, Try}
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

abstract class AsyncWriter[Client, V, E](threads: Int) extends Serializable {

  def readRecord(client: Client, key: String): Try[V]

  def encodeRecord(key: String, value: V): E

  def writeRecord(client: Client, key: String, encoded: E): Try[Long]

  def write(
    client: Client,
    partition: Iterator[(String, V)],
    mergeFunc: Option[(V, V) => V] = None,
    retryFunc: Option[Throwable => Boolean]
  ): Unit = {
    if (partition.isEmpty) return

    val pool = Executors.newFixedThreadPool(threads)

    val rows: Process[Task, (String, V)] =
      Process.unfold(partition){ iter =>
        if (iter.hasNext) {
          Some((iter.next, iter))
        } else None
      }

    def elaborateRow(row: (String, V)): Process[Task, (String, V)] = {
      val foldUpdate: ((String, V)) => (String, V) = { case newRecord @ (key, newValue) =>
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

      Process eval Task(foldUpdate(row))(pool)
    }

    def encode(row: (String, V)): Process[Task, (String, E)] = {
      val (key, value) = row
      val encodeTask = Task((key, encodeRecord(key, value)))(pool)
      Process.eval(encodeTask)
    }


    def retire(row: (String, E)): Process[Task, Try[Long]] = {
      val writeTask = Task(writeRecord(client, row._1, row._2))(pool)
      import geotrellis.spark.util.TaskUtils._
      Process.eval(retryFunc.fold(writeTask)(writeTask.retryEBO(_)))
    }

    val results = nondeterminism.njoin(maxOpen = threads, maxQueued = threads) {
      rows flatMap elaborateRow flatMap encode map retire
    }(Strategy.Executor(pool))

    results.run.unsafePerformSync
    pool.shutdown()
  }
}
