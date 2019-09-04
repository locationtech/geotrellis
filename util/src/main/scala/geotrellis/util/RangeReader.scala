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

import scala.collection.JavaConverters._
import java.net.URI
import java.util.ServiceLoader

import scala.util.Try

/**
 * This trait defines methods for breaking up a source of bytes into
 * Map[Long, Array[Byte]] called a, "chunk". Where the Long is where within
 * the file the chunk begins and the Array[Byte] containing the actual bytes.
 */
trait RangeReader {
  def totalLength: Long

  private def clipToSize(start: Long, length: Int): Int =
    if (start + length <= totalLength)
      length
    else
      (totalLength - start).toInt

  protected def readClippedRange(start: Long, length: Int): Array[Byte]

  def readRange(start: Long, length: Int): Array[Byte] =
    readClippedRange(start, clipToSize(start, length))

  /** Gets the entire object as an Array.
    * This will fail if objectLength > Int.MaxValue
    */
  def readAll(): Array[Byte] =
    readClippedRange(0, totalLength.toInt)
}

object RangeReader {
  implicit def rangeReaderToStreamingByteReader(rangeReader: RangeReader): StreamingByteReader =
    StreamingByteReader(rangeReader)

  implicit def rangeReaderToStreamingByteReaderOpt(rangeReader: Option[RangeReader]): Option[StreamingByteReader] =
    rangeReader.map(rangeReaderToStreamingByteReader)

  def apply(uri: URI): RangeReader =
    ServiceLoader.load(classOf[RangeReaderProvider])
      .iterator().asScala
      .find(_.canProcess(uri))
      .getOrElse(throw new RuntimeException(s"Unable to find RangeReaderProvider for $uri"))
      .rangeReader(uri)

  def apply(uri: String): RangeReader = apply(new URI(uri))

  /** This function checks if the source is valid, by trying to read the first byte of the data. */
  def validated(uri: URI): Option[RangeReader] = Try { apply(uri).readRange(0, 1); apply(uri) }.toOption

  def validated(uri: String): Option[RangeReader] = validated(new URI(uri))
}
