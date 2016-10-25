package geotrellis.util

import java.io._

object ResourceReader {
  def asString(filename: String): String = {
    val stream: InputStream = getClass.getResourceAsStream(s"/$filename")
    try { scala.io.Source.fromInputStream( stream ).getLines.mkString(" ") } finally { stream.close() }
  }
}
