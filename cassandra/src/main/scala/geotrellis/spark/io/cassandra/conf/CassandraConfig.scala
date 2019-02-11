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

package geotrellis.spark.io.cassandra.conf

import geotrellis.spark.io.hadoop.conf.CamelCaseConfig
import geotrellis.spark.util._
import pureconfig.EnumCoproductHint

case class CassandraCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class CassandraRDDConfig(write: String = "default", read: String = "default") {
  def readThreads: Int = threadsFromString(read)
  def writeThreads: Int = threadsFromString(write)
}

case class CassandraThreadsConfig(
  collection: CassandraCollectionConfig = CassandraCollectionConfig(),
  rdd: CassandraRDDConfig = CassandraRDDConfig()
)

sealed trait CassandraIndexStrategy

/**
  * The default range query strategy.  Spreads tiles evenly across the Cassandra
  * cluster, but makes no attempt to leverage locality guarantees provided by
  * Space-Filling Curve index.  Efficient for heavy write loads, but probably
  * less efficient for bulk reads.
  */
case object WriteOptimized extends CassandraIndexStrategy

/**
  * The read-optimized query strategy will attempt to bin zoom levels
  * into intelligently sized chunks by range-binning the SFC index. May
  * be more efficient for workloads with high read/throughput requirements.
  * Be sure to also set 'tilesPerPartition' to something reasonable if using
  * this partitioning strategy.  "Reasonable" here will depend on the size
  * of the tiles you're storing and your Cassandra cluster's tolerance for
  * large partition sizes.  Be sure to stress test and tweak as necessary to
  * find an appropriate value for your application.
  */
case object ReadOptimized extends CassandraIndexStrategy

case class CassandraConfig(
  port: Int = 9042,
  catalog: String = "metadata",
  keyspace: String = "geotrellis",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1,
  localDc: String = "datacenter1",
  usedHostsPerRemoteDc: Int = 0,
  allowRemoteDCsForLocalConsistencyLevel: Boolean = false,
  threads: CassandraThreadsConfig = CassandraThreadsConfig(),
  indexStrategy: CassandraIndexStrategy = WriteOptimized,
  tilesPerPartition: Int = 1 //Has no effect unless Read-Optimized-Query is used.
)

object CassandraConfig extends CamelCaseConfig {
  private implicit lazy val indexStrategyHint = new EnumCoproductHint[CassandraIndexStrategy]
  lazy val conf: CassandraConfig = pureconfig.loadConfigOrThrow[CassandraConfig]("geotrellis.cassandra")
  implicit def cassandraConfigToClass(obj: CassandraConfig.type): CassandraConfig = conf
}
