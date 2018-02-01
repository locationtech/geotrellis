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

import org.scalatest._

import java.nio.ByteBuffer

class MockRangeReader(arr: Array[Byte]) extends RangeReader {
  var numberOfReads = 0
  def totalLength = arr.length
  def readClippedRange(start: Long, length: Int): Array[Byte] = {
    numberOfReads += 1
    arr.slice(start.toInt, (start + length).toInt)
  }
}

class StreamingByteReaderSpec extends FunSpec with Matchers {
  describe("StreamingByteReader") {
    val arr = Array.ofDim[Byte](Byte.MaxValue * 100)
    for(chunk <- 0 until 100;
        byte <- 0 until Byte.MaxValue) {
      arr(chunk*Byte.MaxValue + byte) = byte.toByte
    }

    it("should start with a position of 0") {
      val br = new StreamingByteReader(new MockRangeReader(arr))

      br.position should be (0)
    }

    it("should not read upon move within current change") {
      val mockRangeReader = new MockRangeReader(arr)
      val br = new StreamingByteReader(mockRangeReader, chunkSize = 10)
      br.position(0)
      br.position(8)

      mockRangeReader.numberOfReads should be (0)
    }

    it("should read the correct byte after moving position") {
      val br = new StreamingByteReader(new MockRangeReader(arr))
      br.position(5)
      br.get should be (5.toByte)
    }

    it("should read into the next chunk") {
      val mockRangeReader = new MockRangeReader(arr)
      val br = new StreamingByteReader(mockRangeReader, chunkSize = 10)
      br.get
      br.position(9)
      br.getInt // reads 4 bytes

      mockRangeReader.numberOfReads should be (2)
    }

    it("should read an int across chunks") {
      val intArray = Array(1, 2, 3, 4, 5, 6, 7).map(Int.MaxValue / 2 - _)
      val arr = new Array[Byte](intArray.size * 4)
      val bytebuff = ByteBuffer.wrap(arr)
      bytebuff.asIntBuffer.put(intArray)
      val mockRangeReader = new MockRangeReader(arr)
      val br = new StreamingByteReader(mockRangeReader, chunkSize = 10)

      br.position(8)
      val result = br.getInt

      result should be (Int.MaxValue / 2 - 3)
    }

    it("should read only one chunk on initial position move and read") {
      val mockRangeReader = new MockRangeReader(arr)
      val br = new StreamingByteReader(mockRangeReader, chunkSize = 10)
      br.position(11)
      br.get

      mockRangeReader.numberOfReads should be (1)
    }

    it("should only retrieve one chunk if doing a get and getBytes that is within the chunk") {
      val mockRangeReader = new MockRangeReader(arr)
      val br = new StreamingByteReader(mockRangeReader, chunkSize = 10)
      br.get
      br.position(1)
      val result = br.getBytes(4)

      mockRangeReader.numberOfReads should be (1)
      result.toSeq should be (Seq(1, 2, 3, 4))
    }

    it("should retrieve 5 bytes if requesting a large sequence 5 bytes from the end of the reader") {
      val mockRangeReader = new MockRangeReader(arr)
      val br = new StreamingByteReader(mockRangeReader, chunkSize = 10)
      br.position(arr.length - 5)
      val result = br.getBytes(40)

      mockRangeReader.numberOfReads should be (1)
      result.toSeq should be (Seq(122, 123, 124, 125, 126))
    }
  }
}
