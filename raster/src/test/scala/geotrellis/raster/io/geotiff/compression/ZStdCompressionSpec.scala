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

import com.github.luben.zstd.{Zstd, ZstdException}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

import java.util.Random

class ZStdCompressionSpec extends AnyFunSpec with Matchers {
  describe("ZStdCompression") {
    (-1 to 9).foreach { level =>
      it(s"produces complete Zstandard frames for incompressible and empty segments at level $level") {
        val random = new Random(1234)
        val inputs = Seq(96 * 96, 65536, 1, 0).map { size =>
          val bytes = new Array[Byte](size)
          random.nextBytes(bytes)
          bytes
        }
        val compressor = ZStdCompression(level).createCompressor(inputs.size)
        val decompressor = compressor.createDecompressor()

        inputs.zipWithIndex.foreach { case (input, index) =>
          withClue(s"segment size ${input.length}: ") {
            val compressed = compressor.compress(input, index)
            Zstd.findFrameCompressedSize(compressed) shouldBe compressed.length.toLong

            val decoded = new Array[Byte](input.length)
            val size = Zstd.decompress(decoded, compressed)
            withClue(s"Zstandard decompression: ${Zstd.getErrorName(size)}: ") {
              Zstd.isError(size) shouldBe false
            }
            size shouldBe input.length.toLong
            decoded should equal(input)
            decompressor.decompress(compressed, index) should equal(input)

            intercept[ZstdException] {
              Zstd.findFrameCompressedSize(compressed.dropRight(1))
            }
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
        val compressor = new ZStdCompression(level).createCompressor(1)
        val compressed = compressor.compress(segment, 0)
        val decompressor = compressor.createDecompressor()
        val decompressed = decompressor.decompress(compressed, 0)
        decompressed should be(segment)
      }
    }
  }
}
