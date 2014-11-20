package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import java.nio.{ByteBuffer, ByteOrder}

import java.nio.charset.{Charset => JavaCharset}

object Charset {
  val Ascii = Charset("US-ASCII")
}

case class Charset(code: String)

object CodePageFileReader {

  def apply(path: String): CodePageFileReader =
    if (path.endsWith(".cpg")) apply(Filesystem.slurp(path))
    else apply(Array[Byte]())

  def apply(bytes: Array[Byte]): CodePageFileReader =
    new CodePageFileReader(new String(bytes))

}

class CodePageFileReader(charset: String) {

  import Charset._

  lazy val read: Charset =
    if (JavaCharset.availableCharsets.containsKey(charset)) Charset(charset)
    else Ascii

}
