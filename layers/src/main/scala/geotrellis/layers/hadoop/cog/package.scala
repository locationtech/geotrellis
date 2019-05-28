package geotrellis.layers.hadoop

import geotrellis.util.StreamingByteReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI


package object cog {
  def byteReader(uri: URI, conf: Configuration = new Configuration): StreamingByteReader =
    StreamingByteReader(HdfsRangeReader(new Path(uri), conf))

  private[cog]
  def makePath(chunks: String*) =
    chunks
      .collect { case str if str.nonEmpty => if(str.endsWith("/")) str.dropRight(1) else str }
      .mkString("/")
}
