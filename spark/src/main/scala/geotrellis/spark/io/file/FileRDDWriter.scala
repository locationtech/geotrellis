package geotrellis.spark.io.file

import geotrellis.raster.io.Filesystem
import geotrellis.spark.io.avro.{AvroRecordCodec, AvroEncoder}
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import java.io.File

class FileRDDWriter [K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag]() {

  val codec  = KeyValueRecordCodec[K, V]
  val schema = codec.schema

  def write(rdd: RDD[(K, V)], rootPath: String, keyPath: K => String, oneToOne: Boolean): Unit = {
    val _codec = codec

    val pathsToTiles =
      if (oneToOne) {
        rdd.map { case row @ (key, value) => (keyPath(key), Vector(row)) }
      } else {
        rdd.groupBy { case (key, _) => keyPath(key) }
      }

    val f = new File(rootPath)
    if(f.exists) {
      require(f.isDirectory, s"$rootPath exists but is not a directory.")
    } else {
      f.mkdirs()
    }

    pathsToTiles.foreach { case (path, rows) =>
      val bytes = AvroEncoder.toBinary(rows.toVector)(_codec)
      if(!new File(path).getParentFile.exists) { sys.error("WAAAAA") }
      Filesystem.writeBytes(path, bytes)
    }
  }
}
