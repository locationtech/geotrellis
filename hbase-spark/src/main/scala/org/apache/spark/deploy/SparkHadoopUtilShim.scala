/*
 * Copyright 2020 Azavea
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

package org.apache.spark.deploy

import org.apache.spark.SparkConf
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

/** A proxy object for https://github.com/apache/spark/blob/v3.0.1/core/src/main/scala/org/apache/spark/deploy/SparkHadoopUtil.scala#L425-L429 */
object GTSparkHadoopUtil {

  def get: SparkHadoopUtil = SparkHadoopUtil.get

  /**
   * Returns a Configuration object with Spark configuration applied on top. Unlike
   * the instance method, this will always return a Configuration instance, and not a
   * cluster manager-specific type.
   */
  def newConfiguration(conf: SparkConf): Configuration = SparkHadoopUtil.newConfiguration(conf)

  // scalastyle:off line.size.limit
  /**
   * Create a file on the given file system, optionally making sure erasure coding is disabled.
   *
   * Disabling EC can be helpful as HDFS EC doesn't support hflush(), hsync(), or append().
   * https://hadoop.apache.org/docs/r3.0.0/hadoop-project-dist/hadoop-hdfs/HDFSErasureCoding.html#Limitations
   */
  // scalastyle:on line.size.limit
  def createFile(fs: FileSystem, path: Path, allowEC: Boolean): FSDataOutputStream =
    SparkHadoopUtil.createFile(fs, path, allowEC)
 }
