package geotrellis.spark.io.hadoop

import geotrellis.util.RangeReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

/**
 * This class extends [[RangeReader]] by reading chunks out of a GeoTiff on HDFS.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @return A new instance of S3RangeReader.
 */
class HdfsRangeReader(
  path: Path,
  conf: Configuration) extends RangeReader {

  val totalLength: Long =
    path.getFileSystem(conf).getFileStatus(path).getLen

  def readClippedRange(start: Long, length: Int): Array[Byte] =
    HdfsUtils.readRange(path, start, length, conf)
}

object HdfsRangeReader {
  /**
    * Returns a new instance of HdfsRangeReader.
    *
    * @param path: Path to file
    * @param conf: Hadoop configuration
    *
    * @return A new instance of HdfsRangeReader.
    */
  def apply(path: Path, conf: Configuration): HdfsRangeReader =
    new HdfsRangeReader(path, conf)

}
