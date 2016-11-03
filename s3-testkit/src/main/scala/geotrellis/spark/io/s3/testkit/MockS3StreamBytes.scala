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

package geotrellis.spark.io.s3.testkit

import geotrellis.util._
import geotrellis.spark.io.s3._

import com.amazonaws.services.s3.model._

class MockS3ArrayBytes(val chunkSize: Int, testArray: Array[Byte])
  extends MockS3StreamBytes {

  def objectLength: Long = testArray.length.toLong

  def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk =
      if (!pastLength(length + start))
        length
      else
        objectLength - start

    val newArray = Array.ofDim[Byte](chunk.toInt)
    System.arraycopy(testArray, start.toInt, newArray, 0, chunk.toInt)
    accessCount += 1

    arrayPosition = (start + length).toInt
    
    newArray
  }
}
  
class MockS3Stream(val chunkSize: Int, length: Long,
  r: GetObjectRequest) extends MockS3StreamBytes {
  val mockClient = new MockS3Client

  def objectLength = length

  override def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk =
      if (start + length <= objectLength)
        start + length
      else
        objectLength

    mockClient.readRange(start, chunk, r)
  }
}

trait MockS3StreamBytes {

  var arrayPosition = 0
  var accessCount = 0

  def chunkSize: Int
  def objectLength: Long

  def pastLength(chunkSize: Long): Boolean =
    if (chunkSize > objectLength) true else false
  
  def getArray: Array[Byte] =
    getArray(arrayPosition)

  def getArray(start: Long): Array[Byte] =
    getArray(arrayPosition, chunkSize)

  def getArray(start: Long, length: Long): Array[Byte]

  def getMappedArray(): Map[Long, Array[Byte]] =
    getMappedArray(arrayPosition, chunkSize)

  def getMappedArray(start: Long): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Long, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))
}
