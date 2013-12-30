package geotrellis.spark.rdd

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.Scanner
import scala.collection.mutable.ListBuffer
import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.hadoop.fs.Path
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.tiling.TileBounds
import org.apache.hadoop.fs.FileSystem
import java.io.PrintWriter
import java.io.ObjectInputStream
import org.apache.hadoop.io.ObjectWritable
import java.io.ObjectOutputStream
import scala.collection.mutable.ArrayBuffer

class TileIdPartitioner extends org.apache.spark.Partitioner {

  @transient
  var splitPoints = new Array[TileIdWritable](0)

  override def getPartition(key: Any) = findPartition(key)
  override def numPartitions = splitPoints.length
  override def toString = splitPoints.zipWithIndex.mkString
  
  // TODO override equals and hashCode
  
  private def findPartition(key: Any) = {
    val index = java.util.Arrays.binarySearch(splitPoints.asInstanceOf[Array[Object]], key)
    println("findPartition returned " + index)
    if (index < 0)
      (index + 1) * -1
    else
      index
  }


  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeInt(splitPoints.length)
    splitPoints.foreach(split => out.writeLong(split.get))
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()    
    val buf = new ArrayBuffer[TileIdWritable]
    val len = in.readInt
    for (i <- 0 until len)
      buf += TileIdWritable(in.readLong())
    splitPoints = buf.toArray
  }
}

object TileIdPartitioner {
  val SplitFile = "splits"

  def apply(splitFile: String, conf: Configuration): TileIdPartitioner = {
    val tp = new TileIdPartitioner
    tp.splitPoints = readSplits(splitFile, conf)
    tp
  }

  def apply(splitGenerator: SplitGenerator, splitFile: String, conf: Configuration): TileIdPartitioner = {
    writeSplits(splitGenerator, new Path(splitFile), conf)
    apply(splitFile, conf)
  }

  private def readSplits(splitFile: String, conf: Configuration): Array[TileIdWritable] = {
    val splitPoints = new ListBuffer[TileIdWritable]
    val splitFilePath = new Path(splitFile)
    val fs = splitFilePath.getFileSystem(conf)
    var fdis: Option[FSDataInputStream] = None
    var in: Option[Scanner] = None

    if (fs.getClass == classOf[LocalFileSystem]) {
      // local file 
      val localSplitFile = new File(splitFilePath.toUri().getPath())
      if (localSplitFile.exists()) {
        in = Some(new Scanner(new BufferedReader(new FileReader(localSplitFile))))
      }
    } else {
      // hdfs file 
      if (fs.exists(splitFilePath)) {
        fdis = Some(fs.open(splitFilePath))
        in = Some(new Scanner(new BufferedReader(new InputStreamReader(fdis.get))))
      }

    }

    if (in.isDefined) {
      try {
        while (in.get.hasNextLine)
          splitPoints += TileIdWritable(ByteBuffer.wrap(Base64.decodeBase64(in.get.nextLine.getBytes)).getLong)
      } finally {
        in.get.close()
      }
    }

    if (fdis.isDefined) {
      fdis.get.close()
    }

    splitPoints.toArray
  }
  private def writeSplits(splitGenerator: SplitGenerator, splitFile: Path, conf: Configuration): Int = {
    val splits = splitGenerator.getSplits
    println("writing splits to " + splitFile)
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

  def printSplits(splitFile: String, conf: Configuration) {
    val splits = readSplits(splitFile, conf)
    splits.zipWithIndex.foreach(t => println("Split #%d: %d".format(t._2, t._1.get)))
  }
}

trait SplitGenerator {
  def getSplits: Seq[Long]
}
case class ImageSplitGenerator(
  tileBounds: TileBounds,
  zoom: Int,
  increment: Int = -1)
  extends SplitGenerator {
  // if increment is -1 getSplits return an empty sequence
  def getSplits = for (i <- tileBounds.s until tileBounds.n by increment) yield TmsTiling.tileId(tileBounds.e, i, zoom)
}

object ImageSplitGenerator {
  def apply(tileBounds: TileBounds, zoom: Int, tileSizeBytes: Int, blockSizeBytes: Int) = {
    new ImageSplitGenerator(tileBounds, zoom, computeIncrement(tileBounds, tileSizeBytes, blockSizeBytes))
  }
  def computeIncrement(tileBounds: TileBounds, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toLong
    val tileCount = tileBounds.width * tileBounds.height

    // return -1 if it doesn't make sense to have splits, getSplits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        -1
      else
        (tilesPerBlock / tileBounds.width).toInt
    increment
  }
}

