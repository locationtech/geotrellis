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

package geotrellis.spark.io.file

import geotrellis.layers._
import geotrellis.layers.file.{FileAttributeStore, FileValueReader}
import geotrellis.spark.io._
import geotrellis.spark.testkit.TestEnvironment

import org.scalatest._


class FileLayerProviderSpec extends FunSpec with TestEnvironment {
  val uri = new java.net.URI("file:/tmp/catalog")
  it("construct FileAttributeStore from URI"){
    val store = AttributeStore(uri)
    assert(store.isInstanceOf[FileAttributeStore])
  }

  it("construct FileLayerReader from URI") {
    val reader = LayerReader(uri)
    assert(reader.isInstanceOf[FileLayerReader])
  }

  it("construct FileLayerWriter from URI") {
    val reader = LayerWriter(uri)
    assert(reader.isInstanceOf[FileLayerWriter])
  }

  it("construct FileValueReader from URI") {
    val reader = ValueReader(uri)
    assert(reader.isInstanceOf[FileValueReader])
  }

  it("should be able to process a URI without a scheme") {
    val badURI = new java.net.URI("/tmp/catalog")
    val provider = new FileSparkLayerProvider

    provider.canProcess(badURI) should be (true)
  }
}
