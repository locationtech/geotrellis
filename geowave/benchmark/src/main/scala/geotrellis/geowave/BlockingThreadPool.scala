/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{Blocker, ContextShift, IO, Sync, Timer}
import cats.syntax.either._
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import pureconfig._
import pureconfig.module.catseffect._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object BlockingThreadPool extends Serializable {
  case class Config(threads: Int = Runtime.getRuntime.availableProcessors)

  implicit val configReader: ConfigReader[Config] = { cur =>
    cur.fluent.at("threads").asString match {
      case Right("default") => Config().asRight
      case Right(th) => Try(th.toInt) match {
        case Success(threads) => Config(threads).asRight
        case Failure(_) => Config().asRight
      }
      case Left(_) => Config().asRight
    }
  }

  lazy val load: Config = ConfigSource.default.at("geotrellis.geowave.blocking-thread-pool").loadOrThrow[Config]
  def loadF[F[_]: Sync]: F[Config] = ConfigSource.default.at("geotrellis.geowave.blocking-thread-pool").loadF[F, Config]
  implicit def blockingThreadPoolToClass(obj: BlockingThreadPool.type): Config = load

  lazy val pool: ExecutorService =
    Executors.newFixedThreadPool(
      load.threads,
      new BasicThreadFactory.Builder().namingPattern("geoindex-default-io-%d").build()
    )

  lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutor(pool)
  lazy val blocker: Blocker = Blocker.liftExecutionContext(executionContext)

  lazy val contextShiftIO: ContextShift[IO] = IO.contextShift(executionContext)
  lazy val timerIO: Timer[IO] = IO.timer(executionContext)
}
