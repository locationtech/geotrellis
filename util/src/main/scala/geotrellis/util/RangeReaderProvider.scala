package geotrellis.util

import java.net.URI


trait RangeReaderProvider {
  def canProcess(uri: URI): Boolean

  def rangeReader(uri: URI): RangeReader
}
