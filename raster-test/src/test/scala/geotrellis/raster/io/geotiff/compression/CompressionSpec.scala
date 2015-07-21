package geotrellis.raster.io.geotiff.compression

import org.scalatest._

class CompressionSpec extends FunSpec with Matchers {
  describe("DeflateCompression") {
    it("should decompress and compress to the same things") {
      val segment = """
the human machine only moves when the tide of every one of us pushes to the outside
and we realize that from the time you wake until the time you shake it up
you're nothing more than a ghost echo
it burns brighter when the pieces move the best way,
whatever best may mean,
it's so subjective. but when enough agree we can finally move it.
when the pieces understand what they're doing they can chose a direction
and run a little hotter.
against inertia,
it goes.
whole lives change
only the moment remains here
""".getBytes("UTF-8")
      val compressor = DeflateCompression.createCompressor(1)
      val compressed = compressor.compress(segment, 0)
      val decompressor = compressor.createDecompressor()
      val decompressed = decompressor.decompress(compressed, 0)
      decompressed should be (segment)
    }
  }
}
