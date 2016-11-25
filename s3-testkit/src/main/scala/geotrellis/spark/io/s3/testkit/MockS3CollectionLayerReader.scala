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

package geotrellis.spark.io.s3.testkit

import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import org.apache.spark._

class MockS3CollectionLayerReader(
  attributeStore: AttributeStore
)(implicit sc: SparkContext) extends S3CollectionLayerReader(attributeStore) {
  override def collectionReader =
    new S3CollectionReader {
      def getS3Client = () => new MockS3Client()
    }
}
