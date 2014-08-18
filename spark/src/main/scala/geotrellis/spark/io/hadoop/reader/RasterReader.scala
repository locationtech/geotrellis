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

package geotrellis.spark.io.hadoop.reader

import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.utils.SparkUtils

import org.apache.hadoop.io._
import org.apache.hadoop.conf._
import org.apache.hadoop.fs._

import java.io.Closeable

/* 
 * An Iterable-based reader which supports range lookups (e.g., [10, 20] and 
 * point lookups (e.g., [10, 10]). Internally, it uses the corresponding 
 * MapFile API to seek to the correct start tileId and stop once it seeks 
 * past the end tileId
 * 
 */
case class RasterReader(
  raster: Path,
  conf: Configuration,
  startKey: TileId = Long.MinValue,
  endKey: TileId = Long.MaxValue
) extends Iterable[WritableTile] with Closeable {

  def close = iterator.close

  def iterator = new Iterator[WritableTile] with Closeable {

    private var curKey: TileIdWritable = TileIdWritable(startKey)
    private var curValue: ArgWritable = new ArgWritable

    // initialize readers and partitioner
    private val readers = getReaders
    private val partitioner = TileIdPartitioner(HadoopUtils.readSplits(raster, conf))

    private var readFirstKey: Boolean = false
    private var curPartition: Int = -1

    // find the partition containing the first key in the range
    // if found, set curPartition to its partition
    {
      var partition = partitioner.getPartition(startKey)
      while (curPartition == -1 && partition < partitioner.numPartitions) {
        curKey = 
          readers(partition)
            .getClosest(TileIdWritable(startKey), curValue)
            .asInstanceOf[TileIdWritable]
        if (curKey != null) {
          readFirstKey = true
          curPartition = partition
        } else {
          partition += 1
        }
      }
    }

    def close = readers.foreach(r => if (r != null) r.close)

    // TODO - rewrite to remove early returns 
    override def hasNext: Boolean = {
      if (curKey == null)
        return false
      if (readFirstKey) {
        readFirstKey = false
        // handle boundary case: startKey >= endKey
        if (curKey.compareTo(TileIdWritable(endKey)) <= 0) {
          return true
        }
        return false
      }
      /*
       *  1. found = readers[curPartition].next(currentKey, value)
       *  2. if !found increment curPartition, ensure that its within limits, and 
       *  run 1. again. if its not within limits, return false
       *  3. if currentKey <= endKey return true, else return false;
       */
      while (true) {
        val found = readers(curPartition).next(curKey, curValue)
        if (found) {
          if (curKey.compareTo(TileIdWritable(endKey)) <= 0)
            return true
          else
            return false
        } else {
          curPartition += 1
          if(curPartition >= partitioner.numPartitions) {
            return false
          }
        }
      }
      return true
    }

    override def next = (curKey, curValue)

    private def getReaders: Array[MapFile.Reader] = {
      val fs = raster.getFileSystem(conf)
      val dirs = FileUtil.stat2Paths(fs.listStatus(raster)).sortBy(_.toUri.toString)

      def isData(fst: FileStatus) = fst.getPath.getName.equals("data")

      def isMapFileDir(path: Path) = 
        fs
          .listStatus(path)
          .find(isData(_)) match {
            case Some(f) => true
            case None    => false
          }

      val readers = for {
        dir <- dirs
        if (isMapFileDir(dir))
      } yield new MapFile.Reader(fs, dir.toUri().toString(), conf)

      readers
    }

  }
}
