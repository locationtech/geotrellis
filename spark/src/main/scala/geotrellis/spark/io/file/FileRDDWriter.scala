package geotrellis.spark.io.file

import geotrellis.raster.io.Filesystem
import geotrellis.spark.io.avro.{AvroRecordCodec, AvroEncoder}
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import java.io.File

object FileRDDWriter {
  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    rootPath: String,
    keyPath: K => String
  ): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    // TODO: Is there a way to reduce the shuffle?
    val pathsToTiles =
      rdd.groupBy { case (key, _) => keyPath(key) }

    Filesystem.ensureDirectory(rootPath)

    pathsToTiles.foreach { case (path, rows) =>
      val bytes = AvroEncoder.toBinary(rows.toVector)(codec)
      Filesystem.writeBytes(path, bytes)
    }
  }
}
