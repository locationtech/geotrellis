/*
 * Copyright 2021 Azavea
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

import org.scalatest.funspec.AnyFunSpec

import scala.util.Properties
import org.locationtech.geowave.core.store.api.DataStore
import org.locationtech.geowave.core.store.api.DataStoreFactory
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}

class TestEnvironment extends AnyFunSpec {
  val kafka: String     = Properties.envOrElse("KAFKA_HOST", "localhost:9092")
  val cassandra: String = Properties.envOrElse("CASSANDRA_HOST", "localhost")

  def getDataStore(name: String): DataStore = {
    DataStoreFactory.createDataStore(
      new CassandraRequiredOptions(cassandra, name,
      new CassandraOptions()))
  }
}
