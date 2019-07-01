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

package geotrellis.store.conf

import pureconfig._

import scala.util.{Failure, Success, Try}

case class BlockingThreadPoolConfig(threads: Int = Runtime.getRuntime.availableProcessors)

object BlockingThreadPoolConfig {
  implicit val blockingThreadPoolConfigReader = ConfigReader.fromCursor[BlockingThreadPoolConfig] { cur =>
    cur.fluent.at("threads").asString match {
      case Right("default") => Right(BlockingThreadPoolConfig())
      case Right(th) => Try(th.toInt) match {
        case Success(threads) => Right(BlockingThreadPoolConfig(threads))
        case Failure(_) => Right(BlockingThreadPoolConfig())
      }
      case Left(_) => Right(BlockingThreadPoolConfig())
    }
  }

  lazy val conf: BlockingThreadPoolConfig = pureconfig.loadConfigOrThrow[BlockingThreadPoolConfig]("geotrellis.blocking-thread-pool")
  implicit def blockingThreadPoolConfigToClass(obj: BlockingThreadPoolConfig.type): BlockingThreadPoolConfig = conf
}
