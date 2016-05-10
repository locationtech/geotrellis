from geotrellis.spark.io.avro.codecs.TileCodecs import (
        ByteArrayTileCodec,
        FloatArrayTileCodec,
        DoubleArrayTileCodec,
        ShortArrayTileCodec,
        IntArrayTileCodec,
        BitArrayTileCodec,
        UByteArrayTileCodec,
        UShortArrayTileCodec)

def tileUnionCodec():
    return AvroUnionCodec(Tile, 
        ByteArrayTileCodec(),
        FloatArrayTileCodec(),
        DoubleArrayTileCodec(),
        ShortArrayTileCodec(),
        IntArrayTileCodec(),
        BitArrayTileCodec(),
        UByteArrayTileCodec(),
        UShortArrayTileCodec())
