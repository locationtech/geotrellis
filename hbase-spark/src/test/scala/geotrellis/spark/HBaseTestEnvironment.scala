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

import geotrellis.store.hbase._
import geotrellis.spark.store.hbase._

import geotrellis.spark.store.kryo.KryoRegistrator
import geotrellis.spark.testkit.TestEnvironment

import org.apache.spark.SparkConf
import org.apache.zookeeper.client.FourLetterWordMain
import org.scalatest._

trait HBaseTestEnvironment extends TestEnvironment { self: Suite =>
  override def setKryoRegistrator(conf: SparkConf) =
    conf.set("spark.kryo.registrator", classOf[KryoRegistrator].getName)
      .set("spark.kryo.registrationRequired", "false")

  override def beforeAll = {
    super.beforeAll
    try {
      // check zookeeper availability
      FourLetterWordMain.send4LetterWord("localhost", 2181, "srvr")
    } catch {
      case e: java.net.ConnectException => {
        println("\u001b[0;33mA script for setting up the HBase environment necessary to run these tests can be found at scripts/hbaseTestDB.sh - requires a working docker setup\u001b[m")
        cancel
      }
    }

    try {
      HBaseInstance(Seq("localhost"), "localhost").withAdminDo {
        _.tableExists("tiles")
      }
    } catch {
      case e: Exception => {
        println("\u001b[0;33mA script for setting up the HBase environment necessary to run these tests can be found at scripts/hbaseTestDB.sh - requires a working docker setup\u001b[m")
        throw e
      }
    }
  }

  beforeAll()
}
