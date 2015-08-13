package geotrellis.spark.io.avro

import geotrellis.spark.SpatialKey
import geotrellis.spark.io.avro.TileCodecs._
import geotrellis.spark.io.avro.KeyCodecs._
import org.scalatest._
import geotrellis.raster._

class TileRecordSpec extends FunSpec with AvroTools {
  describe("TileRecordCodecs") {
    it("encodes (key,tile) pairs"){
      val pairs = Vector(
        SpatialKey(1,2) -> DoubleArrayTile.fill(1,10,12),
        SpatialKey(3,6) -> DoubleArrayTile.fill(2,10,12),
        SpatialKey(3,4) -> DoubleArrayTile.fill(3,10,12)
      )

      val codec = new KeyValueRecordCodec[SpatialKey, DoubleArrayTile]

      roundTrip(pairs)(codec)
    }
  }
}
