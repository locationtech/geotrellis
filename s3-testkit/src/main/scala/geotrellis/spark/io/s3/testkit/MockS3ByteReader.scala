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

import java.nio.{ByteBuffer, Buffer, ByteOrder}
import com.amazonaws.services.s3.model._

import spire.syntax.cfor._

class MockS3ByteReader(mock: MockS3StreamBytes,
  order: Option[ByteOrder]) extends ByteReader {
  private var chunk = mock.getMappedArray(0)
  private def offset = chunk.head._1
  private def chunkArray = chunk.head._2
  var accessCount = 0
  def length = chunkArray.length

  var chunkBuffer = newByteBuffer(chunkArray)

  def position = offset + chunkBuffer.position

  def position(newPoint: Long): Buffer = {
    if (isContained(newPoint)) {
      chunkBuffer.position((newPoint - offset).toInt)
    } else {
      accessCount += 1
      adjustChunk(newPoint)
      chunkBuffer.position(0)
    }
  }

  private def adjustChunk: Unit =
    adjustChunk(position)

  private def adjustChunk(newPoint: Long): Unit = {
    accessCount += 1
    chunk = mock.getMappedArray(newPoint)
    chunkBuffer = newByteBuffer
  }
  
  def get: Byte = {
    if (chunkBuffer.position + 1 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.get
  }
  
  def getChar: Char = {
    if (chunkBuffer.position + 2 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getChar
  }

  def getShort: Short = {
    if (chunkBuffer.position + 2 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getShort
  }
  
  def getInt: Int = {
    if (chunkBuffer.position + 4 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getInt
  }

  def getFloat: Float = {
    if (chunkBuffer.position + 4 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getFloat
  }
  
  def getDouble: Double = {
    if (chunkBuffer.position + 8 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getDouble
  }
  
  def getLong: Long = {
    if (chunkBuffer.position + 8 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getLong
  }
  
  def getByteBuffer: ByteBuffer =
    chunkBuffer

  def newByteBuffer: ByteBuffer =
    newByteBuffer(chunkArray)

  def newByteBuffer(byteArray: Array[Byte]) =
    order match {
      case Some(x) => ByteBuffer.wrap(byteArray).order(x)
      case None => ByteBuffer.wrap(byteArray)
    }

  def isContained(newPosition: Long): Boolean =
    if (newPosition >= offset && newPosition <= offset + length) true else false
}
