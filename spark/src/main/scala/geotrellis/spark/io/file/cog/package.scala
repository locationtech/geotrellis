package geotrellis.spark.io.file

import geotrellis.util.{ByteReader, Filesystem}

import java.net.URI

package object cog extends Implicits {
  def byteReader(uri: URI): ByteReader =
    Filesystem.toMappedByteBuffer(uri.getPath)
}
