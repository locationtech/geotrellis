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

import geotrellis.spark.store.s3.testkit._
import geotrellis.proj4.LatLng
import geotrellis.raster.Tile
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.vector.{Extent, ProjectedExtent}

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.core.sync.RequestBody
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{Job, RecordReader, TaskAttemptContext, InputSplit}
import org.scalatest._

import scala.collection.JavaConverters._

class MockS3InputFormat extends S3InputFormat[ProjectedExtent, Array[Byte]] {
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new S3RecordReader[ProjectedExtent, Array[Byte]](getS3Client(context)) {
      def read(key: String, obj: Array[Byte]) =
        ProjectedExtent(Extent.fromString(key), LatLng) -> obj
    }
}

class S3InputFormatSpec extends FunSpec with Matchers {
  describe("S3 InputFormat") {

    it("should parse the s3 url containing bucket name with dash") {
      val url = "s3n://nex-bcsd-tiled-geotiff/prefix/subfolder"
      val S3InputFormat.S3UrlRx(id, key, bucket, prefix) = url
      id should be (null)
      key should be (null)
      bucket should be ("nex-bcsd-tiled-geotiff")
      prefix should be ("prefix/subfolder")
    }

    it("should parse the s3 url containing bucket name with period") {
      val url = "s3n://nex.bcsd.tiled.geotiff/prefix/subfolder"
      val S3InputFormat.S3UrlRx(id, key, bucket, prefix) = url
      id should be (null)
      key should be (null)
      bucket should be ("nex.bcsd.tiled.geotiff")
      prefix should be ("prefix/subfolder")
    }

    it("should parse the s3 url containing keys") {
      // don't get too excited, not real keys
      val url = "s3n://AAIKJLIB4YGGVMAATT4A:ZcjWmdXN+75555bptjE4444TqxDY3ESZgeJxGsj8@nex-bcsd-tiled-geotiff/prefix/subfolder"
      val S3InputFormat.S3UrlRx(id, key, bucket, prefix) = url

      id should be ("AAIKJLIB4YGGVMAATT4A")
      key should be ("ZcjWmdXN+75555bptjE4444TqxDY3ESZgeJxGsj8")
      bucket should be ("nex-bcsd-tiled-geotiff")
      prefix should be ("prefix/subfolder")
    }

    it("should parse s3 url without keys"){
      val url = "s3n://nex-bcsd-tiled-geotiff/prefix/subfolder"
      val  S3InputFormat.S3UrlRx(id, key, bucket, prefix) = url

      id should be (null)
      key should be (null)
      bucket should be ("nex-bcsd-tiled-geotiff")
      prefix should be ("prefix/subfolder")
    }

    val mockClient = MockS3Client()
    S3TestUtils.cleanBucket(mockClient, "s3-input-format")
    for (i <- 1 to 10) {
      val extent = Extent(i, i, i, i)
      val putReq = PutObjectRequest.builder()
        .bucket("s3-input-format")
        .key(s"keys/${extent.toString}")
        .build()
      val putBody = RequestBody.fromBytes(Array.fill[Byte](i)(1))
      mockClient.putObject(putReq, putBody)
    }

    it("should divide keys evenly between partitions"){
      val job = Job.getInstance(new Configuration())
      S3InputFormat.setCreateS3Client(job, () => MockS3Client())
      S3InputFormat.setUrl(job, "s3n://s3-input-format/keys")
      S3InputFormat.setAnonymous(job)
      S3InputFormat.setPartitionCount(job, 5)

      val format = new MockS3InputFormat
      val splits = format.getSplits(job).asScala.toVector
      val lengths = splits.map(_.getLength)
      all (lengths) should be (2)
      lengths.sum should be (10)
    }

    it("should divide keys evenly by size"){
      val job = Job.getInstance(new Configuration())
      S3InputFormat.setCreateS3Client(job, () => MockS3Client())
      S3InputFormat.setUrl(job, "s3n://s3-input-format/keys")
      S3InputFormat.setAnonymous(job)
      S3InputFormat.setPartitionBytes(job, 21)

      val format = new MockS3InputFormat
      val splits = format.getSplits(job).asScala.toVector.asInstanceOf[Vector[S3InputSplit]]
      splits.foreach(s => assert(s.size <= 21))
      splits.map(_.size).sum should be ((1 to 10).sum)
    }
  }
}
