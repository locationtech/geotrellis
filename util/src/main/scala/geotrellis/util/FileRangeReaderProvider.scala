package geotrellis.util

import java.net.URI
import java.nio.file.Paths
import java.io.File


class FileRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def rangeReader(uri: URI): FileRangeReader =
    FileRangeReader(Paths.get(uri.toString).toFile)
}
