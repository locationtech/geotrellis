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
import geotrellis.spark.cmd.args.RasterArgs
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils._
import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import com.quantifind.sumac.ArgMain

class TileIdPartitioner extends org.apache.spark.Partitioner {

  @transient
  private var splits = new Array[TileIdWritable](0)

  override def getPartition(key: Any): Int = findPartition(key)
  override def numPartitions = splits.length + 1
  override def toString = "TileIdPartitioner split points: " + {
    if (splits.isEmpty) "Empty" else splits.zipWithIndex.mkString
  }

  // get min,max tileId in a given partition
  def range(partition: Int): (TileIdWritable, TileIdWritable) = {
    val min = if (partition == 0) Long.MinValue else splits(partition - 1).get + 1
    val max = if (partition == splits.length) Long.MaxValue else splits(partition).get
    (TileIdWritable(min), TileIdWritable(max))
  }

  def save(raster: Path, conf: Configuration) =
    TileIdPartitioner.writeSplits(splits.toSeq.map(_.get), raster, conf)

  def splitGenerator = new SplitGenerator {
    def getSplits = splits.map(_.get)
  }
  
  override def equals(other: Any): Boolean = 
    other match {
      case that: TileIdPartitioner => that.splits.deep == splits.deep
      case _                       => false
    }
  
  override def hashCode: Int = splits.hashCode
  
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

object TileIdPartitioner extends ArgMain[RasterArgs] {
  final val SplitFile = "splits"

  /* construct a partitioner from the splits file, if one exists */
  def apply(raster: Path, conf: Configuration): TileIdPartitioner = {
    val tp = new TileIdPartitioner
    tp.splits = readSplits(raster, conf)
    tp
  }

  /* construct a partitioner from the splits file, if one exists */
  def apply(splits: Array[TileIdWritable]): TileIdPartitioner = {
    val tp = new TileIdPartitioner
    tp.splits = splits
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

  private def writeSplits(splitGenerator: SplitGenerator, raster: Path, conf: Configuration): Int =
    writeSplits(splitGenerator.getSplits, raster, conf)

  private def writeSplits(splits: Seq[Long], raster: Path, conf: Configuration): Int = {
    val splitFile = new Path(raster, SplitFile)
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

  private def printSplits(raster: Path, conf: Configuration): Unit = {
    val zoom = raster.getName().toInt
    val splits = readSplits(raster, conf)
    splits.zipWithIndex.foreach {
      case (tileId, index) => {
        val (tx, ty) = TmsTiling.tileXY(tileId.get, zoom)
        println(s"Split #${index}: tileId=${tileId.get}, tx=${tx}, ty=${ty}")
      }
    }

    // print the increments unless there were no splits
    if (!splits.isEmpty) {
      val meta = PyramidMetadata(raster.getParent(), conf)
      val tileExtent = meta.metadataForBaseZoom.tileExtent
      val (tileSize, cellType) = (meta.tileSize, meta.cellType)

      val inc = RasterSplitGenerator.computeIncrement(tileExtent,
        TmsTiling.tileSizeBytes(tileSize, cellType),
        HdfsUtils.defaultBlockSize(raster, conf))
      println(s"(xInc,yInc) = ${inc}")
    }
  }

  def main(args: RasterArgs) {
    val input = new Path(args.inputraster)
    val conf = SparkUtils.createHadoopConfiguration
    printSplits(input, conf)
  }
}
