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

package geotrellis.store.util

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import pureconfig._

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object BlockingThreadPool extends Serializable {
  case class Config(threads: Int = Runtime.getRuntime.availableProcessors)

  implicit val configReader = ConfigReader.fromCursor[Config] { cur =>
    cur.fluent.at("threads").asString match {
      case Right("default") => Right(Config())
      case Right(th) => Try(th.toInt) match {
        case Success(threads) => Right(Config(threads))
        case Failure(_) => Right(Config())
      }
      case Left(_) => Right(Config())
    }
  }

  lazy val conf: Config = ConfigSource.default.at("geotrellis.blocking-thread-pool").loadOrThrow[Config]
  implicit def blockingThreadPoolToConf(obj: BlockingThreadPool.type): Config = conf

  @transient lazy val pool: ExecutorService =
    Executors.newFixedThreadPool(
      conf.threads,
      new BasicThreadFactory.Builder().namingPattern("geotrellis-default-io-%d").build()
    )
  @transient lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutor(pool)
}
