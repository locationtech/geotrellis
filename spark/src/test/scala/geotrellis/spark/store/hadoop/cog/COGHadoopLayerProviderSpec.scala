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

package geotrellis.spark.store.hadoop.cog

import geotrellis.layers.cog.COGValueReader
import geotrellis.layers.hadoop.cog.HadoopCOGValueReader
import geotrellis.spark.store._
import geotrellis.spark.store.cog._
import geotrellis.spark.testkit.TestEnvironment

import org.scalatest._

class COGHadoopLayerProviderSpec extends FunSpec with TestEnvironment {
  val uri = new java.net.URI("hdfs+file:/tmp/catalog")

  it("construct HadoopCOGLayerReader from URI") {
    val reader = COGLayerReader(uri)
    assert(reader.isInstanceOf[HadoopCOGLayerReader])
  }

  it("construct HadoopCOGLayerWriter from URI") {
    val reader = COGLayerWriter(uri)
    assert(reader.isInstanceOf[HadoopCOGLayerWriter])
  }

  it("construct HadoopCOGValueReader from URI") {
    val reader = COGValueReader(uri)
    assert(reader.isInstanceOf[HadoopCOGValueReader])
  }

  it("should not be able to process a URI without a scheme") {
    val badURI = new java.net.URI("/tmp/catalog")
    val provider = new HadoopCOGSparkLayerProvider

    provider.canProcess(badURI) should be (false)
  }
}
