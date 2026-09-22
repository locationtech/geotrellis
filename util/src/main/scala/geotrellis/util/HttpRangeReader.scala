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

package geotrellis.util

import org.log4s.*

import java.io.{ByteArrayOutputStream, InputStream}
import java.net.{HttpURLConnection, URI, URL}

import scala.util.Try

/**
  * This class extends [[RangeReader]] by reading chunks out of a GeoTiff at the
  * specified HTTP location.
  *
  * @throws [[HttpStatusException]] if the HTTP response code is 4xx or 5xx
  *
  * @param url: A [[URL]] pointing to the desired GeoTiff.
  */
class HttpRangeReader(url: URL, useHeadRequest: Boolean) extends RangeReader {
  @transient private[this] lazy val logger = getLogger

  private def open(method: String, requestHeaders: Seq[(String, String)] = Nil): HttpURLConnection = {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod(method)
    requestHeaders.foreach { case (name, value) => connection.setRequestProperty(name, value) }
    connection
  }

  private def readBody(connection: HttpURLConnection): Array[Byte] = {
    val stream: InputStream =
      if (connection.getResponseCode >= 400) connection.getErrorStream
      else connection.getInputStream

    if (stream == null) Array.empty[Byte]
    else
      try {
        val out = new ByteArrayOutputStream()
        val buffer = Array.ofDim[Byte](8192)
        var read = stream.read(buffer)
        while (read != -1) {
          out.write(buffer, 0, read)
          read = stream.read(buffer)
        }
        out.toByteArray
      } finally stream.close()
  }

  private def throwError(connection: HttpURLConnection): Unit = {
    val code = connection.getResponseCode
    if (code >= 400)
      throw new HttpStatusException(code, connection.getResponseMessage, new String(readBody(connection)))
  }

  val totalLength: Long = {
    val connection = open(if (useHeadRequest) "HEAD" else "GET")
    try {
      val contentLength = Option(connection.getHeaderField("Content-Length"))
        .flatMap(cl => Try(cl.toLong).toOption) match {
          case Some(num) => num
          case None => -1L
      }
      throwError(connection)

      /**
        * "The Accept-Ranges response HTTP header is a marker used by the server
        *  to advertise its support of partial requests. The value of this field
        *  indicates the unit that can be used to define a range."
        * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges
        */
      require(Option(connection.getHeaderField("Accept-Ranges")).contains("bytes"),
        "Server doesn't support ranged byte reads")

      require(contentLength > 0,
        "Server didn't provide (required) \"Content-Length\" headers, unable to do range-based read")

      contentLength
    } finally connection.disconnect()
  }

  def readClippedRange(start: Long, length: Int): Array[Byte] = {
    val connection = open("GET", Seq("Range" -> s"bytes=${start}-${start + length}"))
    try {
      val code = connection.getResponseCode

      /**
        * "If the byte-range-set is unsatisfiable, the server SHOULD return
        *  a response with a status of 416 (Requested range not satisfiable).
        *  Otherwise, the server SHOULD return a response with a status of 206
        *  (Partial Content) containing the satisfiable ranges of the entity-body."
        * https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
        */
      require(code != 416,
        "Server unable to generate the byte range between ${start} and ${start + length}")

      if (code != 206) logger.info("Server responded to range request with HTTP code other than PARTIAL_RESPONSE (206)")

      readBody(connection)
    } finally connection.disconnect()
  }

}

/** The companion object of [[HttpRangeReader]] */
object HttpRangeReader {

  def apply(address: String): HttpRangeReader = apply(URI.create(address))

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

  def withoutHeadRequest(address: String): HttpRangeReader = withoutHeadRequest(URI.create(address))

  def withoutHeadRequest(uri: URI): HttpRangeReader = withoutHeadRequest(uri.toURL)
}
