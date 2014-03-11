/***
 * Copyright (c) 2014 Digital Globe.
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
 ***/

package geotrellis.spark.rdd
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.utils._

import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.hadoop.fs.Path

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.Scanner

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

class TileIdPartitioner extends org.apache.spark.Partitioner {

  @transient
  private var splits = new Array[TileIdWritable](0)

  override def getPartition(key: Any) = findPartition(key)
  override def numPartitions = splits.length + 1
  override def toString = "TileIdPartitioner split points: " + {
    if (splits.isEmpty) "Empty" else splits.zipWithIndex.mkString
  }

  // get min,max tileId in a given partition
  def range(partition: Int): (TileIdWritable, TileIdWritable) = {
    val min = if(partition == 0) Long.MinValue else splits(partition-1).get + 1
    val max = if(partition == splits.length) Long.MaxValue else splits(partition).get
    (TileIdWritable(min), TileIdWritable(max))
  }
  
  // TODO override equals and hashCode
  
  private def findPartition(key: Any) = {
    val index = java.util.Arrays.binarySearch(splits.asInstanceOf[Array[Object]], key)
    if (index < 0)
      (index + 1) * -1
    else
      index
  }

  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeInt(splits.length)
    splits.foreach(split => out.writeLong(split.get))
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    val buf = new ArrayBuffer[TileIdWritable]
    val len = in.readInt
    for (i <- 0 until len)
      buf += TileIdWritable(in.readLong())
    splits = buf.toArray
  }
}

object TileIdPartitioner {
  final val SplitFile = "splits"

  /* construct a partitioner from the splits file, if one exists */
  def apply(raster: Path, conf: Configuration): TileIdPartitioner = {
    val tp = new TileIdPartitioner
    tp.splits = readSplits(raster, conf)
    tp
  }

  /* construct a partitioner from the splits file, if one exists */
  def apply(raster: String, conf: Configuration): TileIdPartitioner = {
    apply(new Path(raster), conf)
  }

  /* construct a partitioner from a split generator */
  def apply(splitGenerator: SplitGenerator, raster: Path, conf: Configuration): TileIdPartitioner = {
    writeSplits(splitGenerator, raster, conf)
    apply(raster, conf)
  }

  private def readSplits(raster: Path, conf: Configuration): Array[TileIdWritable] = {
    val splitFile = new Path(raster, SplitFile)
    HdfsUtils.getLineScanner(splitFile, conf) match {
      case Some(in) =>
        try {
          val splits = new ListBuffer[TileIdWritable]
          for (line <- in) {
            splits +=
              TileIdWritable(ByteBuffer.wrap(Base64.decodeBase64(line.getBytes)).getLong)
          }
          splits.toArray
        } finally {
          in.close
        }
      case None =>
        Array[TileIdWritable]()
    }
  }

  private def writeSplits(splitGenerator: SplitGenerator, raster: Path, conf: Configuration): Int = {
    val splits = splitGenerator.getSplits
    val splitFile = new Path(raster, SplitFile)
    //println("writing splits to " + splitFile)
    val fs = splitFile.getFileSystem(conf)
    val fdos = fs.create(splitFile)
    val out = new PrintWriter(fdos)
    splits.foreach {
      split => out.println(new String(Base64.encodeBase64(ByteBuffer.allocate(8).putLong(split).array())))
    }
    out.close()
    fdos.close()

    splits.length
  }

  def printSplits(raster: Path, conf: Configuration) {
    val splits = readSplits(raster, conf)
    splits.zipWithIndex.foreach(t => println("Split #%d: %d".format(t._2, t._1.get)))
  }
}

