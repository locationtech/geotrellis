package geotrellis.spark.io.file

import geotrellis.util.{ByteReader, Filesystem}

import java.net.URI

package object cog {
  def byteReader(uri: URI): ByteReader =
    Filesystem.toMappedByteBuffer(uri.getPath)
}
