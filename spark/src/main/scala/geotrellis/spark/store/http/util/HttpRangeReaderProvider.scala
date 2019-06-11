package geotrellis.spark.store.http.util

import geotrellis.util.RangeReaderProvider

import java.net.URI


class HttpRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean =
    SCHEMES.contains(uri.getScheme) && urlValidator.isValid(uri.toString)

  def rangeReader(uri: URI): HttpRangeReader =
    HttpRangeReader(uri)
}
