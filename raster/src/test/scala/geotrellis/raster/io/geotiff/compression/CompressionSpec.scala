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

package geotrellis.raster.io.geotiff.compression

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

import java.util.Random
import java.util.zip.Inflater

class CompressionSpec extends AnyFunSpec with Matchers {
  describe("DeflateCompression") {
    def assertCompleteStream(compressed: Array[Byte], expected: Array[Byte]): Unit = {
      val inflater = new Inflater()
      try {
        inflater.setInput(compressed)
        val decoded = new Array[Byte](expected.length + 1)
        var size = 0
        while (!inflater.finished()) {
          val n = inflater.inflate(decoded, size, decoded.length - size)
          size += n
          withClue(s"Incomplete zlib stream (needsInput=${inflater.needsInput()}, needsDictionary=${inflater.needsDictionary()}): ") {
            assert(n > 0 || inflater.finished())
          }
        }
        size shouldBe expected.length
        java.util.Arrays.copyOf(decoded, size) should equal(expected)
        inflater.getRemaining shouldBe 0
      } finally inflater.end()
    }

    (-1 to 9).foreach { level =>
      it(s"produces complete zlib streams for incompressible and empty segments at level $level") {
        val random = new Random(1234)
        val inputs = Seq(96 * 96, 65536, 1, 0).map { size =>
          val bytes = new Array[Byte](size)
          random.nextBytes(bytes)
          bytes
        }
        val compressor = DeflateCompression(level).createCompressor(inputs.size)
        val helper = new DeflateDecompressor(Array.emptyIntArray)

        inputs.zipWithIndex.foreach { case (input, index) =>
          withClue(s"segment size ${input.length}: ") {
            assertCompleteStream(compressor.compress(input, index), input)
          }
        }
        inputs.foreach { input =>
          withClue(s"helper, segment size ${input.length}: ") {
            assertCompleteStream(helper.compress(input, level), input)
          }
        }
      }
    }

    (-1 to 9).foreach { level =>
      it(s"should decompress and compress to the same things at the ${if(level == -1) "default compression level" else s"zoom level $level"}") {
        val segment =
          """
            | the human machine only moves when the tide of every one of us pushes to the outside
            | and we realize that from the time you wake until the time you shake it up
            | you're nothing more than a ghost echo
            | it burns brighter when the pieces move the best way,
            | whatever best may mean,
            | it's so subjective. but when enough agree we can finally move it.
            | when the pieces understand what they're doing they can chose a direction
            | and run a little hotter.
            | against inertia,
            | it goes.
            | whole lives change
            | only the moment remains here""".getBytes("UTF-8")
        val compressor = new DeflateCompression(level).createCompressor(1)
        val compressed = compressor.compress(segment, 0)
        val decompressor = compressor.createDecompressor()
        val decompressed = decompressor.decompress(compressed, 0)
        decompressed should be(segment)
      }
    }
  }
}
