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

package geotrellis.spark.io.hdfs.reader

import geotrellis.raster._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.utils._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.hadoop.fs.FileUtil
import org.apache.hadoop.fs.FileStatus

import java.io.Closeable

/* 
 * An Iterable-based reader. Note that even though Iterables have a rich set of methods
 * this implementation currently would iterate through all tiles to perform any of the  
 * operations. So for example last() would iterate through all tiles to find the last  
 * tile. Clearly, this can be optimized further in at least two ways:
 * 
 * 1. Point looksups on tile ids. Internally seeks the MapFile.Reader to the correct 
 * location for fast performance
 * 2. Range lookups on ranges of tiles (with optional start/end values). Internally
 * seeks the MapFile.Reader to the start location and stops past the end of the 
 * user-provided range
 * 
 */ 
case class SimpleRasterReader(raster: Path, conf: Configuration)
  extends Iterable[(SpatialKeyWritable, TileWritable)]
  with Closeable {

  def close = iterator.close

  def iterator = new Iterator[(SpatialKeyWritable, TileWritable)] with Closeable {

    private val curKey: SpatialKeyWritable = new SpatialKeyWritable
    private val curValue: TileWritable = new TileWritable
    private var curPartition: Int = 0

    // initialize readers and partitioner
    private val readers = getReaders
    
    def close = readers.foreach(r => if (r != null) r.close)
    
    override def hasNext = {
      if (curPartition >= readers.length)
        false
      else if (readers(curPartition).next(curKey, curValue))
        true
      else {
        curPartition += 1
        hasNext
      }
    }
    
    override def next = (curKey,curValue)

    private def getReaders: Array[MapFile.Reader] = {
      val fs = raster.getFileSystem(conf)
      val dirs = FileUtil.stat2Paths(fs.listStatus(raster)).sortBy(_.toUri.toString)

      def isData(fst: FileStatus) = fst.getPath.getName.equals("data")

      def isMapFileDir(path: Path) = fs.listStatus(path).find(isData(_)) match {
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

// TODO - replace with test
object SimpleRasterReader {

  def main(args: Array[String]): Unit = {
    val raster = new Path("hdfs://localhost:9000/geotrellis/images/testcostdistance-gt-ingest/10")
    val conf = SparkUtils.hadoopConfiguration
    val reader = SimpleRasterReader(raster, conf)
    var count = 0
    reader.foreach{ case(tw,aw) => {
      println(s"tileId=${tw.get}")
      count += 1
    } } 
    //val (tw,aw) = reader.last
    //println(s"last tile id = ${tw.get}")
    reader.close
    println(s"Got $count records")
  }
}
