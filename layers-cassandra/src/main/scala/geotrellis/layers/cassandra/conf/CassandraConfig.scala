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

package geotrellis.layers.cassandra.conf

import geotrellis.util.CamelCaseConfig
import geotrellis.layers.util._

import pureconfig.generic.auto._

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

case class CassandraConfig(
  port: Int = 9042,
  catalog: String = "metadata",
  keyspace: String = "geotrellis",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1,
  localDc: String = "datacenter1",
  usedHostsPerRemoteDc: Int = 0,
  allowRemoteDCsForLocalConsistencyLevel: Boolean = false,
  threads: CassandraThreadsConfig = CassandraThreadsConfig()
)

object CassandraConfig extends CamelCaseConfig {
  lazy val conf: CassandraConfig = pureconfig.loadConfigOrThrow[CassandraConfig]("geotrellis.cassandra")
  implicit def cassandraConfigToClass(obj: CassandraConfig.type): CassandraConfig = conf
}
