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

import geotrellis.util.ByteReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import software.amazon.awssdk.services.s3.model._

/** Reads the tiff tags of GeoTiffs on S3, avoiding full file read. */
class TiffTagsS3InputFormat extends S3InputFormat[GetObjectRequest, TiffTags] {

  /**
   * Creates a RecordReader that can be used to read [[TiffTags]] from an S3 object.
   *
   * @param split The [[InputSplit]] that defines what records will be in this partition.
   * @param context The [[TaskAttemptContext]] of the InputSplit.
   *
   * @return A [[StreamingS3RecordReader]] that can read [[TiffTags]] of GeoTiffs from S3.
   */
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new StreamingS3RecordReader[GetObjectRequest, TiffTags](getS3Client(context)) {

      /**
       * Read the header of the GeoTiff and return its tiff tags along with the corresponding [[GetObjectRequest]].
       *
       * @param key  The S3 key of the file to be read.
       * @param reader A [[ByteReader]] that must be used to read the actual TiffTags from the file.
       */
      def read(key: String, reader: ByteReader): (GetObjectRequest, TiffTags) = {
        val request = GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build()
        val tiffTags = TiffTagsReader.read(reader)
        (request, tiffTags)
      }
    }
}
