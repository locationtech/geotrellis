package geotrellis.spark.io.hadoop

import geotrellis.spark.io.cog.COGBackend
import geotrellis.util.StreamingByteReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI

package object cog extends geotrellis.spark.io.hadoop.cog.Implicits {
  // Type for ValueReader mixin
  trait HadoopCOGBackend extends COGBackend

  def getReader(uri: URI, conf: Configuration = new Configuration): StreamingByteReader =
    StreamingByteReader(HdfsRangeReader(new Path(uri), conf))
}
