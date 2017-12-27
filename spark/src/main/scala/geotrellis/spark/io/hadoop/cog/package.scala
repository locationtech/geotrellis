package geotrellis.spark.io.hadoop

import geotrellis.util.StreamingByteReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI

package object cog extends geotrellis.spark.io.hadoop.cog.Implicits {
  def byteReader(uri: URI, conf: Configuration = new Configuration): StreamingByteReader =
    StreamingByteReader(HdfsRangeReader(new Path(uri), conf))
}
