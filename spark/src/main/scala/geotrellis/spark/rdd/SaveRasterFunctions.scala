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

package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.spark.formats._
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.spark.Logging
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.RDD
import org.apache.hadoop.fs.Path
import geotrellis.spark.metadata.Context

object SaveRasterFunctions extends Logging {

  def save(raster: RasterRDD, rasterPath: Path): Unit = {
    val pyramidPath = rasterPath.getParent()

    logInfo("Saving RasterRDD out...")
    val jobConf = new JobConf(raster.context.hadoopConfiguration)
    jobConf.set("io.map.index.interval", "1");
    SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

    val writableRDD =
      raster.sortByKey().map(TmsTile(_).toWritable)

    writableRDD.saveAsHadoopFile(
      rasterPath.toUri().toString(),
      classOf[TileIdWritable],
      classOf[ArgWritable],
      classOf[MapFileOutputFormat],
      jobConf)

    logInfo(s"Finished saving raster to ${rasterPath}")

    val Context(meta, partitioner) = raster.opCtx 
    meta.save(pyramidPath, raster.context.hadoopConfiguration)
    partitioner.save(rasterPath, raster.context.hadoopConfiguration)
    logInfo(s"Finished saving metadata to ${pyramidPath} and partitioner to ${rasterPath}")
  }
}
