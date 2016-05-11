from tests.geotrellis.spark.io.avro.AvroTools import AvroTools
from geotrellis.spark.io.avro.codecs.KeyValueRecordCodec import KeyValueRecordCodec
from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
from geotrellis.spark.SpatialKey import SpatialKey
from spec import Spec

class TileRecordSpec(AvroTools, Spec):
    "TileRecordCodecs"

    def test_pairs(self):
        "encodes (key,tile) pairs"

        pairs = [(SpatialKey(1,2), DoubleArrayTile.fill(1,10,12)),
                (SpatialKey(3,6), DoubleArrayTile.fill(2,10,12)),
                (SpatialKey(3,4), DoubleArrayTile.fill(3,10,12))]

        codec = KeyValueRecordCodec(SpatialKey, DoubleArrayTile)

        self.roundTrip(pairs, codec)
