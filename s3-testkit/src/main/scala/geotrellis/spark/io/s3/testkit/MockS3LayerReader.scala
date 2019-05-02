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

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.json._

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.regions.Region
import org.apache.spark._
import spray.json._

import java.net.URI

class MockS3LayerReader(
  attributeStore: AttributeStore
)(implicit sc: SparkContext) extends S3LayerReader(attributeStore) {
  override def rddReader =
    new S3RDDReader {
      def getS3Client = () => MockS3Client()
    }
}
