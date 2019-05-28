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

package geotrellis.layers.hadoop.formats

import geotrellis.layers._
import geotrellis.layers.hadoop._
import geotrellis.layers.index.MergeQueue

import org.apache.hadoop.conf._
import org.apache.hadoop.fs._
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

import scala.collection.JavaConverters._
import scala.reflect._


object FilterMapFileInputFormat {
  // Define some key names for Hadoop configuration
  val FILTER_INFO_KEY = "geotrellis.spark.io.hadoop.filterinfo"

  type FilterDefinition = Array[(BigInt, BigInt)]

  def layerRanges(layerPath: Path, conf: Configuration): Vector[(Path, BigInt, BigInt)] = {
    val file = layerPath
      .getFileSystem(conf)
      .globStatus(new Path(layerPath, "*"))
      .filter(_.isDirectory)
      .map(_.getPath)
    mapFileRanges(file, conf)
  }

  def mapFileRanges(mapFiles: Seq[Path], conf: Configuration): Vector[(Path, BigInt, BigInt)] = {
    // finding the max index for each file would be very expensive.
    // it may not be present in the index file and will require:
    //   - reading the index file
    //   - opening the map file
    //   - seeking to data file to max stored index
    //   - reading data file to the final records
    // this is not the type of thing we can afford to do on the driver.
    // instead we assume that each map file runs from its min index to min index of next file

    val fileNameRx = ".*part-r-([0-9]+)-([0-9]+)$".r
    def readStartingIndex(path: Path): BigInt = {
      path.toString match {
        case fileNameRx(part, firstIndex) =>
          BigInt(firstIndex)
        case _ =>
          val indexPath = new Path(path, "index")
          val in = new SequenceFile.Reader(conf, SequenceFile.Reader.file(indexPath))
          val minKey = new BigIntWritable(Array[Byte](0))
          try {
            in.next(minKey)
          } finally { in.close() }
          BigInt(minKey.getBytes)
      }
    }

    mapFiles
      .map { file => readStartingIndex(file) -> file }
      .sortBy(_._1)
      .foldRight(Vector.empty[(Path, BigInt, BigInt)]) {
        case ((minIndex, fs), vec @ Vector()) =>
          (fs, minIndex, BigInt(-1)) +: vec // XXX
        case ((minIndex, fs), vec) =>
          (fs, minIndex, vec.head._2 - 1) +: vec
      }
  }
}
