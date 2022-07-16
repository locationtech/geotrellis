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

package geotrellis.store.cassandra.conf

import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class CassandraConfig(
  port: Int = 9042,
  catalog: String = "metadata",
  keyspace: String = "geotrellis",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1
) {
  def replicationOptions: Map[String, AnyRef] = Map(
    "class" -> replicationStrategy,
    "replication_factor" -> Int.box(replicationFactor)
  )
}

object CassandraConfig {
  lazy val conf: CassandraConfig = ConfigSource.default.at("geotrellis.cassandra").loadOrThrow[CassandraConfig]
  implicit def cassandraConfigToClass(obj: CassandraConfig.type): CassandraConfig = conf
}
