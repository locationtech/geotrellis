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

package geotrellis.spark.etl.s3

import geotrellis.spark.etl._
import geotrellis.spark.etl.config._
import geotrellis.spark.io.s3.S3InputFormat

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext

abstract class S3Input[I, V] extends InputPlugin[I, V] {
  val name = "s3"

  def configuration(input: Input)(implicit sc: SparkContext): Configuration = {
    val profile = input.backend.profile match {
      case Some(sp: S3Profile) => sp
      case _ => throw new Exception("Profile type not matches backend type")
    }
    val path = getPath(input.backend)
    val job = Job.getInstance(sc.hadoopConfiguration, "S3 GeoTiff ETL")
    S3InputFormat.setBucket(job, path.bucket)
    S3InputFormat.setPrefix(job, path.prefix)
    profile.partitionsCount.foreach(S3InputFormat.setPartitionCount(job, _))
    profile.partitionsBytes.foreach(S3InputFormat.setPartitionBytes(job, _))
    job.getConfiguration
  }
}
