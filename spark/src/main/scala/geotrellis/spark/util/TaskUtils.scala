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

package geotrellis.spark.util


import java.util.concurrent.Executors
import scala.concurrent.duration._
import scalaz._
import scalaz.concurrent._
import scalaz.stream._
import scalaz.stream.async._
import collection.JavaConversions._
import scala.util.Random

object TaskUtils extends App {
   implicit class TaskBackoff[A](task: Task[A]) {
    /**
     * Implement non-blocking Exponential Backoff on a Task.
     * @param  p  returns true for exceptions that trigger a backoff and retry
     */
    def retryEBO(p: (Throwable => Boolean) ): Task[A] = {
      def help(count: Int): Future[Throwable \/ A] = {
        val base: Duration = 52.milliseconds
        val timeout = base * Random.nextInt(math.pow(2,count).toInt) // .extInt is [), implying -1
        task.get flatMap {
          case -\/(e) if p(e) =>
            help(count + 1) after timeout
          case x => Future.now(x)
        }
      }
      Task.async { help(0).unsafePerformAsync }
    }
  }
}
