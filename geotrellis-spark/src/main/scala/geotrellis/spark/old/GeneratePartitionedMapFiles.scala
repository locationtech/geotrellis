/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.spark

import geotrellis.raster.IntArrayRasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.SplitGenerator
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.SparkUtils
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.io.SequenceFile
import scala.util.Random
import geotrellis.spark.formats.ArgWritable

/**
 * @author akini
 *
 * A simple example demonstrating reading and writing of (tileId, Array) out to sequence file
 * Run command: GeneratePartitionedMapFiles [HDFS-OR-LOCAL-PATH-TO-DIR] [NUMTILES] [NUMFILES]
 *  		e.g.,	GeneratePartitionedMapFiles file:///tmp/argtest/ 100 5
 * This would give 5 partitions, each having 20 tiles
 *
 *
 */
object GeneratePartitionedMapFiles {
  val defaultTileSize = TmsTiling.DefaultTileSize

  def fill(value: Int) = {
    //val arr = Array.fill(defaultTileSize * defaultTileSize)(value)
    val arr = (Seq.fill(defaultTileSize * defaultTileSize)(Random.nextInt)).toArray
    IntArrayRasterData(arr, defaultTileSize, defaultTileSize)
  }

  def main(args: Array[String]): Unit = {
    val dirPath = new Path(args(0)) // full path to directory
    val numTiles = args(1).toInt
    val numFiles = args(2).toInt

    val tilesPerFile = numTiles / numFiles
    val conf = SparkUtils.createHadoopConfiguration
    conf.set("io.map.index.interval", "1");
    val fs = dirPath.getFileSystem(conf)

    val key = new TileIdWritable()

    for ((indices, fileIndex) <- 0 until numTiles grouped tilesPerFile zipWithIndex) {
      val mapFilePath = new Path(dirPath, f"part-${fileIndex * tilesPerFile}%05d")
      println(s"writing to $mapFilePath")

      val writer = new MapFile.Writer(conf, fs, mapFilePath.toUri.toString,
        classOf[TileIdWritable], classOf[ArgWritable],
        SequenceFile.CompressionType.RECORD)
      try {
        for (i <- indices) {
          key.set(i)
          val array = fill(i)
          val value = ArgWritable.fromRasterData(array)
          writer.append(key, value)
        }
      } finally {
        writer.close
      }

    }

    val generator = new SplitGenerator {
      def getSplits = (for (i <- 0 until numTiles by tilesPerFile) yield i.toLong).drop(1).map(_ - 1)
      println(getSplits)
    }
    TileIdPartitioner(generator, dirPath, conf)
  }

}