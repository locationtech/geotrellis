def tileUnionCodec():
    from geotrellis.raster.Tile import Tile
    from geotrellis.spark.io.avro.AvroUnionCodec import AvroUnionCodec
    from geotrellis.spark.io.avro.codecs.TileCodecs import (
            ByteArrayTileCodec,
            FloatArrayTileCodec,
            DoubleArrayTileCodec,
            ShortArrayTileCodec,
            IntArrayTileCodec,
            BitArrayTileCodec,
            UByteArrayTileCodec,
            UShortArrayTileCodec)
    return AvroUnionCodec(Tile, 
        ByteArrayTileCodec(),
        FloatArrayTileCodec(),
        DoubleArrayTileCodec(),
        ShortArrayTileCodec(),
        IntArrayTileCodec(),
        BitArrayTileCodec(),
        UByteArrayTileCodec(),
        UShortArrayTileCodec())
