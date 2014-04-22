/*
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.cmd

import geotrellis._
import geotrellis.spark.Tile
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.ingest.IngestInputFormat
import geotrellis.spark.ingest.TiffTiler
import geotrellis.spark.metadata.PyramidMetadata

import geotrellis.spark.rdd._
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import org.apache.spark.Logging
import org.apache.spark.SerializableWritable
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid.GridCoverage2D

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required

object HdfsTest extends Logging {

  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  def main(args: Array[String]): Unit = {
    val conf = SparkUtils.createHadoopConfiguration
    val sc = SparkUtils.createSparkContext("local", "HdfsTest")

    val rd = RasterRDD("hdfs://localhost:9000/user/rob/outarg/13", sc)

    rd.save("hdfs://localhost:9000/user/rob/outarg2/13")
  }
}
