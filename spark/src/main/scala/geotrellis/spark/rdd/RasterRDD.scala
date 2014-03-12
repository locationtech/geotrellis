/**************************************************************************
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
 **************************************************************************/

package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.metadata.Context
import geotrellis.spark.op.local.AddOpMethods
import geotrellis.spark.op.local.DivideOpMethods
import geotrellis.spark.op.local.MultiplyOpMethods
import geotrellis.spark.op.local.SubtractOpMethods
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

import org.apache.spark.Partition
import org.apache.spark.SparkContext
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD

import org.apache.hadoop.fs.Path

class RasterRDD(val prev: RDD[Tile], val opCtx: Context)
  extends RDD[Tile](prev)
  with AddOpMethods[RasterRDD]
  with SubtractOpMethods[RasterRDD]
  with MultiplyOpMethods[RasterRDD]
  with DivideOpMethods[RasterRDD] {

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

  def toWritable = 
    mapPartitions({ partition =>
      partition.map { tile =>
        (TileIdWritable(tile.id), ArgWritable.fromRasterData(tile.raster.data))
      }
    }, true)

  def mapTiles(f: Tile => Tile): RasterRDD =
    mapPartitions({ partition =>
      partition.map { tile =>
        f(tile)
      }
    }, true)
    .withContext(opCtx)

  def combineTiles(other: RasterRDD)(f: (Tile,Tile) => Tile): RasterRDD =
    zipPartitions(other, true) { (partition1, partition2) =>
      partition1.zip(partition2).map { case (tile1, tile2) =>
        f(tile1, tile2)
      }
    }
    .withContext(opCtx)
}

object RasterRDD {
  def apply(raster: String, sc: SparkContext): RasterRDD =
    apply(new Path(raster), sc)

  def apply(raster: Path, sc: SparkContext): RasterRDD =
    RasterHadoopRDD(raster, sc).toRasterRDD

def apply(raster: String, sc: SparkContext, addUserNoData: Boolean): RasterRDD =
    apply(new Path(raster), sc, addUserNoData)

  def apply(raster: Path, sc: SparkContext, addUserNoData: Boolean): RasterRDD =
    RasterHadoopRDD(raster, sc).toRasterRDD(addUserNoData)
}
