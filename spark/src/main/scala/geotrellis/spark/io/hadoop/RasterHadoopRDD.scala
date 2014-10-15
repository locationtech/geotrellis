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

package geotrellis.spark.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

import org.apache.spark.SparkContext
import org.apache.spark.rdd.{RDD, NewHadoopRDD}

/*
 * An RDD abstraction of rasters in Spark. This can give back either tuples of either
 * (SpatialKeyWritable, ArgWritable) or (Long, Raster), the latter being the deserialized 
 * form of the former. See companion object 
 */
class RasterHadoopRDD private (sc: SparkContext, conf: Configuration, path: Path)
  extends NewHadoopRDD[SpatialKeyWritable, ArgWritable](
    sc,
    classOf[SequenceFileInputFormat[SpatialKeyWritable, ArgWritable]],
    classOf[SpatialKeyWritable],
    classOf[ArgWritable],
    conf) {
  lazy val metaData = 
    HadoopUtils.readLayerMetaData(path, context.hadoopConfiguration).rasterMetaData

  def toRasterRDD: RasterRDD[SpatialKey] =
    asRasterRDD(metaData) { map { _.toTuple(metaData) } }
}

object RasterHadoopRDD {
  /* path - fully qualified path to the raster (with zoom level)
   * 	e.g., file:///tmp/mypyramid/10 or hdfs:///geotrellis/images/mypyramid/10
   *   
   * sc - the spark context
   */
  def apply(path: String, sc: SparkContext): RasterHadoopRDD =
    apply(new Path(path), sc)

  def apply(path: Path, sc: SparkContext): RasterHadoopRDD = {
    val updatedConf = 
      sc.hadoopConfiguration.withInputPath(path.suffix(HadoopUtils.SEQFILE_GLOB))

    new RasterHadoopRDD(sc, updatedConf, path)
  }
}
