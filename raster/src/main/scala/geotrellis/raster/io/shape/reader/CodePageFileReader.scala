package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.{Charset => JavaCharset}

import java.io.FileNotFoundException


object Charset {
  val Ascii = Charset("US-ASCII")
}

case class Charset(code: String)

object CodePageFileReader {

  val FileExtension = ".cpg"

  def apply(path: String): CodePageFileReader =
    if (path.endsWith(FileExtension)) try {
      apply(Filesystem.slurp(path))
    } catch {
      case e: FileNotFoundException => apply(Array[Byte]())
    } else apply(Array[Byte]())

  def apply(bytes: Array[Byte]): CodePageFileReader =
    new CodePageFileReader(new String(bytes))

}

class CodePageFileReader(charset: String) {

  import Charset._

  lazy val read: Charset =
    if (JavaCharset.availableCharsets.containsKey(charset)) Charset(charset)
    else Ascii

}
