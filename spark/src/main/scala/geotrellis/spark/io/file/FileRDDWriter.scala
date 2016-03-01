package geotrellis.spark.io.file

import geotrellis.spark.io.avro.{AvroRecordCodec, AvroEncoder}
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.util.Filesystem

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

    val pathsToTiles =
      rdd.groupBy { case (key, _) => keyPath(key) }

    Filesystem.ensureDirectory(rootPath)

    pathsToTiles.foreach { case (path, rows) =>
      val bytes = AvroEncoder.toBinary(rows.toVector)(codec)
      Filesystem.writeBytes(path, bytes)
    }
  }
}
