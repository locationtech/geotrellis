/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.store.http.util

import geotrellis.util.RangeReader

import scalaj.http.Http
import com.typesafe.scalalogging.LazyLogging

import java.net.{URL, URI}
import scala.util.Try


/**
 * This class extends [[RangeReader]] by reading chunks out of a GeoTiff at the
 * specified HTTP location.
 *
 * @param url: A [[URL]] pointing to the desired GeoTiff.
 */
class HttpRangeReader(url: URL, useHeadRequest: Boolean) extends RangeReader with LazyLogging {

  val request = Http(url.toString)

  val totalLength: Long = {
    val headers = if(useHeadRequest) {
      request.method("HEAD").asString
    } else {
      request.method("GET").execute { is => "" }
    }
    val contentLength = headers
        .header("Content-Length")
        .flatMap({ cl => Try(cl.toLong).toOption }) match {
          case Some(num) => num
          case None => -1L
        }

    /**
     * "The Accept-Ranges response HTTP header is a marker used by the server
     *  to advertise its support of partial requests. The value of this field
     *  indicates the unit that can be used to define a range."
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges
     */
    require(headers.header("Accept-Ranges") == Some("bytes"),
      "Server doesn't support ranged byte reads")

    require(contentLength > 0,
      "Server didn't provide (required) \"Content-Length\" headers, unable to do range-based read")

    contentLength
  }

  def readClippedRange(start: Long, length: Int): Array[Byte] = {
    val res = request
      .method("GET")
      .header("Range", s"bytes=${start}-${start + length}")
      .asBytes

    /**
     * "If the byte-range-set is unsatisfiable, the server SHOULD return
     *  a response with a status of 416 (Requested range not satisfiable).
     *  Otherwise, the server SHOULD return a response with a status of 206
     *  (Partial Content) containing the satisfiable ranges of the entity-body."
     * https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
     */
    require(res.code != 416,
      "Server unable to generate the byte range between ${start} and ${start + length}")

    if (res.code != 206) logger.info("Server responded to range request with HTTP code other than PARTIAL_RESPONSE (206)")

    res.body
  }

}

/** The companion object of [[HttpRangeReader]] */
object HttpRangeReader {

  def apply(address: String): HttpRangeReader = apply(new URL(address))

  def apply(uri: URI): HttpRangeReader = apply(uri.toURL)

  /**
   * Returns a new instance of HttpRangeReader.
   *
   * @param url: A [[URL]] pointing to the desired GeoTiff.
   * @return A new instance of HttpRangeReader.
   */
  def apply(url: URL): HttpRangeReader = new HttpRangeReader(url, true)

  /**
   * Returns a new instance of HttpRangeReader which does not use HEAD
   * to determine the totalLength.
   *
   * @param url: A [[URL]] pointing to the desired GeoTiff.
   * @return A new instance of HttpRangeReader.
   */
  def withoutHeadRequest(url: URL): HttpRangeReader = new HttpRangeReader(url, false)

  def withoutHeadRequest(address: String): HttpRangeReader = withoutHeadRequest(new URL(address))

  def withoutHeadRequest(uri: URI): HttpRangeReader = withoutHeadRequest(uri.toURL)
}
