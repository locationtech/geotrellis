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

package geotrellis.spark.store.s3

import geotrellis.layers.LayerId
import geotrellis.store.s3._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.store.s3.testkit._

class S3AttributeStoreSpec extends AttributeStoreSpec {
  val bucket = "attribute-store-test-mock-bucket"
  val prefix = "catalog"
  val client = MockS3Client()
  val getS3Client = () => MockS3Client()
  S3TestUtils.cleanBucket(client, bucket)

  lazy val attributeStore = new S3AttributeStore(bucket, prefix, getS3Client)

  it("should handle prefix with ending slash") {
    val bucket = "test-bucket"
    val prefix = "some/key/"
    val as = new S3AttributeStore(bucket, prefix, getS3Client)

    import S3AttributeStore.SEP
    as.attributePath(LayerId("testLayer", 0), "metadata") should be (s"some/key/_attributes/metadata${SEP}testLayer${SEP}0.json")
  }
}
