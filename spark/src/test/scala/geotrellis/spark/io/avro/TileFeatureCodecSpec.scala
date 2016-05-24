package geotrellis.spark.io.avro

import geotrellis.raster._
import geotrellis.spark.io._
import org.scalatest._


class TileFeatureCodecSpec extends FunSpec with Matchers with AvroTools  {
  describe("TileFeatureCodec") {
    val tile = IntArrayTile.fill(0, 10, 10)

    it("should encode/decode a TileFeature of a Tile and a Tile"){
      roundTrip(TileFeature(tile,tile))
    }

    it("should encode/decode a TileFeature of a Tile and a non-Tile"){
      roundTrip(TileFeature(tile,TileFeature(tile,tile)))
    }

  }
}
