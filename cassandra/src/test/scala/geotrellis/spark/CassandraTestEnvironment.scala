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

package geotrellis.spark

import geotrellis.layers.cassandra.BaseCassandraInstance
import geotrellis.spark.io.kryo.KryoRegistrator
import geotrellis.spark.testkit.TestEnvironment

import org.apache.spark.SparkConf
import org.scalatest._

trait CassandraTestEnvironment extends TestEnvironment { self: Suite =>
  override def setKryoRegistrator(conf: SparkConf) =
    conf.set("spark.kryo.registrator", classOf[KryoRegistrator].getName)
        .set("spark.kryo.registrationRequired", "false")

  override def beforeAll = {
    super.beforeAll
    try {
      val session = BaseCassandraInstance(Seq("127.0.0.1")).getSession
      session.closeAsync()
      session.getCluster.closeAsync()
    } catch {
      case e: Exception =>
        println("\u001b[0;33mA script for setting up the Cassandra environment necessary to run these tests can be found at scripts/cassandraTestDB.sh - requires a working docker setup\u001b[m")
        cancel
    }
  }

  beforeAll()
}
