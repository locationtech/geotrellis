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

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect._

object RasterHadoopRDD {
  /* path - fully qualified path to the raster (with zoom level)
   * 	e.g., file:///tmp/mypyramid/10 or hdfs:///geotrellis/images/mypyramid/10
   *   
   * sc - the spark context
   */
  def apply[K: HadoopWritable: Ordering: ClassTag](path: String, sc: SparkContext): RasterRDD[K] =
    apply(path, sc, FilterSet.EMPTY[K])

  def apply[K: HadoopWritable: Ordering: ClassTag](path: String, sc: SparkContext, filters: FilterSet[K]): RasterRDD[K] =
    apply(new Path(path), sc, filters)

  def apply[K: HadoopWritable: Ordering: ClassTag](path: Path, sc: SparkContext): RasterRDD[K] =
    apply(path, sc, FilterSet.EMPTY[K])

  def apply[K: HadoopWritable: Ordering: ClassTag](path: Path, sc: SparkContext, filters: FilterSet[K]): RasterRDD[K] = {
    val keyWritable = implicitly[HadoopWritable[K]]
    import keyWritable.implicits._

    val ordering = implicitly[Ordering[K]]

    val conf = sc.hadoopConfiguration

    val partitioner = 
      KeyPartitioner[K](HadoopUtils.readSplits(path, conf))

    val updatedConf =
      sc.hadoopConfiguration.withInputPath(path.suffix(HadoopUtils.SEQFILE_GLOB))

    val writableRdd: RDD[(keyWritable.Writable, TileWritable)] =
      if(filters.isEmpty) {
        sc.newAPIHadoopRDD[keyWritable.Writable, TileWritable, SequenceFileInputFormat[keyWritable.Writable, TileWritable]](
          updatedConf,
          classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
          classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]], // ¯\_(ツ)_/¯
          classOf[TileWritable]
        )
      } else {
        val includeKey: keyWritable.Writable => Boolean = 
          { writable => 
            filters.includeKey(writable.toValue)
          }

        val includePartition: Partition => Boolean = 
          { partition =>
            val minKey = partitioner.minKey(partition.index)
            val maxKey = partitioner.maxKey(partition.index)

            filters.includePartition(minKey, maxKey)
          }

        new PreFilteredHadoopRDD[keyWritable.Writable, TileWritable](
          sc,
          classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
          classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]],
          classOf[TileWritable],
          updatedConf
        )(includePartition)(includeKey)
      }

    val metaData =
      HadoopUtils.readLayerMetaData(path, updatedConf).rasterMetaData

    asRasterRDD(metaData) {
      writableRdd
        .map { case (keyWritable, tileWritable) =>
          (keyWritable.toValue, tileWritable.toTile(metaData))
         }
    }
  }
}
