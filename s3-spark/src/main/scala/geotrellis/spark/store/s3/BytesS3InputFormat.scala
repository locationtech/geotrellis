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

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

class BytesS3InputFormat extends S3InputFormat[String, Array[Byte]] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = {
    val s3Client = getS3Client(context)
    new S3RecordReader[String, Array[Byte]](s3Client) {
      def read(key: String, bytes: Array[Byte]) = (key, bytes)
    }
  }
}
