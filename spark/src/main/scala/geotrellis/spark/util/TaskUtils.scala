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


import cats.effect._
import cats.syntax.all._

import scala.concurrent.duration._
import scala.util.Random

object TaskUtils extends App {
  /**
    * Implement non-blocking Exponential Backoff on a Task.
    * @param  p  returns true for exceptions that trigger a backoff and retry
    */

  implicit class IOBackoff[A](ioa: IO[A]) {
    def retryEBO(p: (Throwable => Boolean))(implicit timer: Timer[IO]): IO[A] = {
      def help(count: Int): IO[A] = {
        val base: Duration = 52.milliseconds
        val timeout = base * Random.nextInt(math.pow(2, count).toInt) // .extInt is [), implying -1
        val actualDelay = FiniteDuration(timeout.toMillis, MILLISECONDS)

        ioa.handleErrorWith { error =>
          if(p(error)) IO.sleep(actualDelay) *> help(count + 1)
          else IO.raiseError(error)
        }
      }
      help(0)
    }
  }
}
