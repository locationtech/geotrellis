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

package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.{AccumuloProfile, BackendProfile, EtlConf}
import geotrellis.spark.io.accumulo.{AccumuloAttributeStore, AccumuloWriteStrategy, HdfsWriteStrategy, SocketWriteStrategy}

import com.typesafe.scalalogging.LazyLogging

trait AccumuloOutput[K, V, M] extends OutputPlugin[K, V, M] with LazyLogging {
  val name = "accumulo"

  def strategy(profile: Option[BackendProfile]): AccumuloWriteStrategy = profile match {
    case Some(ap: AccumuloProfile) => {
      val strategy = ap.strategy
        .map {
          _ match {
            case "hdfs" => ap.ingestPath match {
              case Some(ingestPath) =>
                HdfsWriteStrategy(ingestPath)
              case None =>
                AccumuloWriteStrategy.DEFAULT
            }
            case "socket" => SocketWriteStrategy()
          }
        }
        .getOrElse(AccumuloWriteStrategy.DEFAULT)
      logger.info(s"Using Accumulo write strategy: $strategy")
      strategy
    }
    case _ => throw new Exception("Backend profile not matches backend type")
  }
  
  def attributes(conf: EtlConf) = AccumuloAttributeStore(getInstance(conf.outputProfile).connector)
}
