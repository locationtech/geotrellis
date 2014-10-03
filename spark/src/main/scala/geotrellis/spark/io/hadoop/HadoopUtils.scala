package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.json._
import geotrellis.spark.json._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import spray.json._

import scala.collection.mutable

import java.nio.ByteBuffer
 import java.io.PrintWriter

object HadoopUtils {
  final val SEQFILE_GLOB = "/*[0-9]*/data"
  final val SPLITS_FILE = "splits"
  final val METADATA_FILE = "metadata.json"

  def readSplits(raster: Path, conf: Configuration): Array[TileId] = {
    val splitFile = new Path(raster, HadoopUtils.SPLITS_FILE)
    HdfsUtils.getLineScanner(splitFile, conf) match {
      case Some(in) =>
        try {
          val splits = new mutable.ListBuffer[TileId]
          for (line <- in) {
            splits +=
            ByteBuffer.wrap(Base64.decodeBase64(line.getBytes)).getLong
          }
          splits.toArray
        } finally {
          in.close
        }
      case None =>
        Array[TileId]()
    }
  }

  def writeSplits(splits: Seq[Long], raster: Path, conf: Configuration): Unit = {
    val splitFile = new Path(raster, SPLITS_FILE)
    val fs = splitFile.getFileSystem(conf)
    val fdos = fs.create(splitFile)
    val out = new PrintWriter(fdos)
    try {
      for(split <- splits) {
        val s = new String(Base64.encodeBase64(ByteBuffer.allocate(8).putLong(split).array()))
        out.println(s)
      }
    } finally {
      out.close()
      fdos.close()
    }
  }

  def readLayerMetaData(path: Path, conf: Configuration): LayerMetaData = {
    val metaDataPath = new Path(path, METADATA_FILE)
    val txt = HdfsUtils.getLineScanner(metaDataPath, conf) match {
      case Some(in) =>
        try {
          in.mkString
        }
        finally {
          in.close
        }
      case None =>
        sys.error(s"oops - couldn't find metadata here - ${metaDataPath.toUri.toString}")
    }
    txt.parseJson.convertTo[LayerMetaData]
  }

  def writeLayerMetaData(metaData: LayerMetaData, path: Path, conf: Configuration): Unit = {
    val metaPath = new Path(path, METADATA_FILE)
    val fs = metaPath.getFileSystem(conf)
    val fdos = fs.create(metaPath)
    val out = new PrintWriter(fdos)
    try {
      out.println(metaData.toJson)
    } finally {
      out.close()
      fdos.close()
    }
  }
}
