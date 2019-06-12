package geotrellis.spark.store.http.util

import geotrellis.util.RangeReaderProvider

import java.net.{URI, URL}


class HttpRangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean =
    try {
      new URL(uri.toString)
      true
    } catch {
      case _: Throwable => false
    }

  def rangeReader(uri: URI): HttpRangeReader =
    HttpRangeReader(uri)
}
