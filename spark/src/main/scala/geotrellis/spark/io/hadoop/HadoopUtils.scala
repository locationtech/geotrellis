package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.collection.mutable

import java.nio.ByteBuffer

object HadoopUtils {
  final val SeqFileGlob = "/*[0-9]*/data"
  final val SplitFile = "splits"

  def readSplits(raster: Path, conf: Configuration): Array[TileId] = {
    val splitFile = new Path(raster, HadoopUtils.SplitFile)
    HdfsUtils.getLineScanner(splitFile, conf) match {
      case Some(in) =>
        try {
          val splits = new mutable.ListBuffer[TileIdWritable]
          for (line <- in) {
            splits +=
            TileIdWritable(ByteBuffer.wrap(Base64.decodeBase64(line.getBytes)).getLong)
          }
          splits.toArray.map(_.get)
        } finally {
          in.close
        }
      case None =>
        Array[TileId]()
    }
  }
}
