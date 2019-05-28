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

package geotrellis.spark.store.s3.cog

import geotrellis.layers._
import geotrellis.layers.cog._
import geotrellis.store.s3._
import geotrellis.store.s3.cog._
import geotrellis.spark.store._
import geotrellis.spark.store.s3._
import geotrellis.spark.store.cog._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.spark.store.s3.testkit._

import org.scalatest._

class COGS3LayerProviderSpec extends FunSpec with TestEnvironment {
  val client = MockS3Client()
  S3TestUtils.cleanBucket(client, "fake-bucket")
  val uri = new java.net.URI("s3://fake-bucket/some-prefix")

  // The provider uses defaults which must be registered to avoid using the normal
  //  aws credential resolution mechanisms
  S3ClientProducer.set(() => MockS3Client())

  it("construct S3COGLayerReader from URI") {
    val reader = COGLayerReader(uri)
    assert(reader.isInstanceOf[S3COGLayerReader])
  }

  it("construct S3COGLayerWriter from URI") {
    val reader = COGLayerWriter(uri)
    assert(reader.isInstanceOf[S3COGLayerWriter])
  }

  it("construct S3COGValueReader from URI") {
    val reader = COGValueReader(uri)
    assert(reader.isInstanceOf[S3COGValueReader])
  }

  it("should not be able to process a URI without a scheme") {
    val badURI = new java.net.URI("//fake-bucket/some-prefix")
    val provider = new S3COGSparkLayerProvider()

    provider.canProcess(badURI) should be (false)
  }
}
