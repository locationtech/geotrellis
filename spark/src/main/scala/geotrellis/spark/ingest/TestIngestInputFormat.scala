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

package geotrellis.spark.old

import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.ingest.IngestInputFormat
import org.apache.hadoop.fs.Path
import scala.collection.JavaConversions._
import org.apache.spark.SparkContext
import geotrellis.Raster
import geotrellis.spark.metadata.PyramidMetadata

object TestIngestInputFormat {
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext("local", "TestIngest")
    val conf = SparkUtils.createHadoopConfiguration
    val inPath = new Path("file:///home/akini/test/big_files/262359N0530600E_V1.tif")
    val (files, meta) = PyramidMetadata.fromTifFiles(inPath, conf)
    meta.writeToJobConf(conf)
    val job = new Job(conf)
    FileInputFormat.addInputPath(job, inPath)

    val format = new IngestInputFormat
    val splits = format.getSplits(job)
    println(splits.size())
    println(splits.mkString("\n"))

    val rdd = sc.newAPIHadoopRDD[Long, Raster, IngestInputFormat](job.getConfiguration(),
      classOf[IngestInputFormat], classOf[Long], classOf[Raster])
    println("rdd has " + rdd.count + " records")
    /*rdd.foreach {
      case (tileId, raster) =>
        println(s"key = ${tileId}")
    }*/
  }
}