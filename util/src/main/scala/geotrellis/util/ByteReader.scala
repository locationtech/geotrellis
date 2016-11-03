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

import java.nio.{Buffer, ByteBuffer}
import scala.language.implicitConversions

/**
 * This trait is a characteristic of instances that can retrieve bytes from some
 * source.
 */
trait ByteReader {
  def position: Long
  def position(i: Long): Buffer

  def get: Byte
  def getChar: Char
  def getShort: Short
  def getInt: Int
  def getFloat: Float
  def getDouble: Double
  def getLong: Long

  def getByteBuffer: ByteBuffer
}

/**
 * The companion object of [[ByteReader]]. This object contains implicit conversion
 * to and from ByteBuffers and ByteReaders.
 */
object ByteReader {
  implicit def byteBuffer2ByteReader(byteBuffer: ByteBuffer): ByteReader = {
    new ByteReader() {
      def position: Long = byteBuffer.position.toLong
      def position(i: Long): Buffer = byteBuffer.position(i.toInt)

      def get = byteBuffer.get
      def getChar = byteBuffer.getChar
      def getShort = byteBuffer.getShort
      def getInt = byteBuffer.getInt
      def getFloat = byteBuffer.getFloat
      def getDouble = byteBuffer.getDouble
      def getLong = byteBuffer.getLong

      def getByteBuffer = byteBuffer
    }
  }

  implicit def toByteBuffer(br: ByteReader): ByteBuffer =
    br.getByteBuffer
}
