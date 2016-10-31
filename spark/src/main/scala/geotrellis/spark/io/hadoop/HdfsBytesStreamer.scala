package geotrellis.spark.io.hadoop

import geotrellis.util.BytesStreamer

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

/**
 * This class extends [[BytesStreamer]] by reading chunks out of a GeoTiff on HDFS.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
 * @return A new instance of S3BytesStreamer.
 */
class HdfsBytesStreamer(
  path: Path,
  conf: Configuration,
  val chunkSize: Int) extends BytesStreamer {

  val objectLength: Long =
    path.getFileSystem(conf).getFileStatus(path).getLen

  def getArray(start: Long, length: Int): Array[Byte] =
    HdfsUtils.readRange(path, start, clipToSize(start, length), conf)
}

object HdfsBytesStreamer {
  def DEFAULT_CHUNK_SIZE = 512 * 512 * 8

  /**
    * Returns a new instance of HdfsBytesStreamer.
    *
    * @param path: Path to file
    * @param conf: Hadoop configuration
    *
    * @return A new instance of HdfsBytesStreamer.
    */
  def apply(path: Path, conf: Configuration): HdfsBytesStreamer =
    new HdfsBytesStreamer(path, conf, DEFAULT_CHUNK_SIZE)

  /**
    * Returns a new instance of HdfsBytesStreamer.
    *
    * @param path: Path to file
    * @param conf: Hadoop configuration
    * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
    *
    * @return A new instance of HdfsBytesStreamer.
    */
  def apply(path: Path, conf: Configuration, chunkSize: Int): HdfsBytesStreamer =
    new HdfsBytesStreamer(path, conf, chunkSize)
}
