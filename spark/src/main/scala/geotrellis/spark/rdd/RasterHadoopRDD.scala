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
import geotrellis.spark.metadata.Context
import geotrellis.spark.metadata.PyramidMetadata

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.NewHadoopRDD

/*
 * An RDD abstraction of rasters in Spark. This can give back either tuples of either
 * (TileIdWritable, ArgWritable) or (Long, Raster), the latter being the deserialized 
 * form of the former. See companion object 
 */
private[spark]
class RasterHadoopRDD private (raster: Path, sc: SparkContext, conf: Configuration)
  extends NewHadoopRDD[TileIdWritable, ArgWritable](
    sc,
    classOf[SequenceFileInputFormat[TileIdWritable, ArgWritable]],
    classOf[TileIdWritable],
    classOf[ArgWritable],
    conf) {

  /*
   * Overriding the partitioner with a TileIdPartitioner 
   */
  override val partitioner = Some(TileIdPartitioner(raster, conf))

  @transient val pyramidPath = raster.getParent()
  val zoom = raster.getName().toInt
  val meta = PyramidMetadata(pyramidPath, conf)

  def toRasterRDD(addUserNoData: Boolean = false): RasterRDD = 
    mapPartitions { partition =>
      partition.map { writableTile =>
        writableTile.toTmsTile(meta, zoom, addUserNoData)
      }
     }
    .withContext(Context(zoom, meta, partitioner.get)) // .get is safe because it can't be 'None'
}

private[rdd]
object RasterHadoopRDD {

  final val SeqFileGlob = "/*[0-9]*/data"

  /* raster - fully qualified path to the raster (with zoom level)
   * 	e.g., file:///tmp/mypyramid/10 or hdfs:///geotrellis/images/mypyramid/10
   *   
   * sc - the spark context
   */
  def apply(raster: String, sc: SparkContext): RasterHadoopRDD =
    apply(new Path(raster), sc)

  def apply(raster: Path, sc: SparkContext): RasterHadoopRDD = {
    val job = new Job(sc.hadoopConfiguration)
    val globbedPath = new Path(raster.toUri().toString() + SeqFileGlob)
    FileInputFormat.addInputPath(job, globbedPath)
    val updatedConf = job.getConfiguration
    new RasterHadoopRDD(raster, sc, updatedConf)
  }
}
