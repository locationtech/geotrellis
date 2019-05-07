package geotrellis.layers.file

import geotrellis.util.{ByteReader, Filesystem}
import java.net.URI


package object cog {
  implicit def strtoURI(str: String): URI = new URI(str)

  def byteReader(uri: URI): ByteReader = byteReader(uri.getPath)
  def byteReader(uri: String): ByteReader = Filesystem.toMappedByteBuffer(uri)
}
