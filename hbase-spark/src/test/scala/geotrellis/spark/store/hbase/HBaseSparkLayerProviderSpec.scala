/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.store.hbase

import geotrellis.store.hbase._
import geotrellis.spark.io._
import geotrellis.spark.store._
import geotrellis.layers.{AttributeStore, ValueReader}
import geotrellis.store.hbase.HBaseTestEnvironment
import geotrellis.spark.testkit.TestEnvironment
import org.scalatest._

class HBaseSparkLayerProviderSpec extends FunSpec with HBaseTestEnvironment {
  val uri = new java.net.URI("hbase://localhost?master=localhost&attributes=attributes&layers=tiles")
  it("construct HBaseAttributeStore from URI"){
    val store = AttributeStore(uri)
    assert(store.isInstanceOf[HBaseAttributeStore])
  }

  it("construct HBaseLayerReader from URI") {
    val reader = LayerReader(uri)
    assert(reader.isInstanceOf[HBaseLayerReader])
  }

  it("construct HBaseLayerWriter from URI") {
    val reader = LayerWriter(uri)
    assert(reader.isInstanceOf[HBaseLayerWriter])
  }

  it("construct HBaseValueReader from URI") {
    val reader = ValueReader(uri)
    assert(reader.isInstanceOf[HBaseValueReader])
  }

  it("should not be able to process a URI without a scheme") {
    val badURI = new java.net.URI("//localhost?master=localhost&attributes=attributes&layers=tiles")
    val provider = new HBaseSparkLayerProvider

    provider.canProcess(badURI) should be (false)
  }
}
