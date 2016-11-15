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

/**
 * This trait defines methods for breaking up a source of bytes into
 * Map[Long, Array[Byte]] called a, "chunk". Where the Long is where within
 * the file the chunk begins and the Array[Byte] containing the actual bytes.
 */
trait BytesStreamer {

  def chunkSize: Int
  def objectLength: Long

  def passedLength(size: Long): Boolean =
    if (size > objectLength) true else false

  def getArray(start: Long): Array[Byte] =
    getArray(start, chunkSize)

  def getArray(start: Long, length: Long): Array[Byte]

  def getMappedArray(start: Long): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Long, length: Long): Map[Long, Array[Byte]] =
    Map(start -> getArray(start, length))
}
