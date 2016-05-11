from tests.geotrellis.spark.io.avro.AvroTools import AvroTools
from geotrellis.raster.ShortArrayTile import ShortArrayTile
from geotrellis.raster.UShortArrayTile import UShortArrayTile
from geotrellis.raster.IntArrayTile import IntArrayTile
from geotrellis.raster.FloatArrayTile import FloatArrayTile
from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
from geotrellis.raster.ByteArrayTile import ByteArrayTile
from geotrellis.raster.UByteArrayTile import UByteArrayTile
from geotrellis.raster.BitArrayTile import BitArrayTile
from geotrellis.raster.ArrayMultibandTile import ArrayMultibandTile
from spec import Spec

class TileCodecsSpec(AvroTools, Spec):
    class inner_tile_codecs(object):
        "TileCodecs"

        def test_byte(self):
            "encodes ByteArrayTile"
            self.roundTripWithNoDataCheck(ByteArrayTile.fill(127, 10, 15))

        def test_ubyte(self):
            "encodes UByteArrayTile"
            self.roundTripWithNoDataCheck(UByteArrayTile.fill(127, 10, 15))

        def test_short(self):
            "encodes ShortArrayTile"
            self.roundTripWithNoDataCheck(ShortArrayTile.fill(45, 10, 15))

        def test_ushort(self):
            "encodes UShortArrayTile"
            self.roundTripWithNoDataCheck(UShortArrayTile.fill(45, 10, 15))

        def test_int(self):
            "encodes IntArrayTile"
            self.roundTripWithNoDataCheck(IntArrayTile.fill(45, 10, 15))

        def test_float(self):
            "encodes FloatArrayTile"
            self.roundTripWithNoDataCheck(FloatArrayTile.fill(532.4, 10, 15))

        def test_double(self):
            "encodes DoubleArrayTile"
            self.roundTripWithNoDataCheck(DoubleArrayTile.fill(53232322.4, 10, 15))

        def test_multiband(self):
            "encodes ArrayMultibandTile"
            tiles = [DoubleArrayTile.fill(53232322.4, 10, 15)] * 3
            thing = ArrayMultibandTile(tiles)
            self.roundTripWithNoDataCheck(thing)

    class inner_no_nodata_tile_codecs(object):
        "No NoData TileCodecs"

        def test_byte(self):
            "encodes ByteArrayTile"
            self.roundTripWithNoDataCheck(ByteArrayTile.fill(127, 10, 15, ByteCellType))

        def test_ubyte(self):
            "encodes UByteArrayTile"
            self.roundTripWithNoDataCheck(UByteArrayTile.fill(127, 10, 15, UByteCellType))

        def test_short(self):
            "encodes ShortArrayTile"
            self.roundTripWithNoDataCheck(ShortArrayTile.fill(45, 10, 15, ShortCellType))

        def test_ushort(self):
            "encodes UShortArrayTile"
            self.roundTripWithNoDataCheck(UShortArrayTile.fill(45, 10, 15, UShortCellType))

        def test_int(self):
            "encodes IntArrayTile"
            self.roundTripWithNoDataCheck(IntArrayTile.fill(45, 10, 15, IntCellType))

        def test_float(self):
            "encodes FloatArrayTile"
            self.roundTripWithNoDataCheck(FloatArrayTile.fill(532.4, 10, 15, FloatCellType))

        def test_double(self):
            "encodes DoubleArrayTile"
            self.roundTripWithNoDataCheck(DoubleArrayTile.fill(53232322.4, 10, 15, DoubleCellType))

        def test_multiband(self):
            "encodes ArrayMultibandTile"
            tiles = [DoubleArrayTile.fill(53232322.4, 10, 15, DoubleCellType)] * 3
            thing = ArrayMultibandTile(tiles)
            self.roundTripWithNoDataCheck(thing)

    class inner_user_defined_tile_codecs(object):
        "UserDefined TileCodecs"

        def test_byte(self):
            "encodes ByteArrayTile"
            self.roundTripWithNoDataCheck(ByteArrayTile.fill(127, 10, 15, ByteUserDefinedNoDataCellType(123)))

        def test_ubyte(self):
            "encodes UByteArrayTile"
            self.roundTripWithNoDataCheck(UByteArrayTile.fill(127, 10, 15, UByteUserDefinedNoDataCellType(123)))

        def test_short(self):
            "encodes ShortArrayTile"
            self.roundTripWithNoDataCheck(ShortArrayTile.fill(45, 10, 15, ShortUserDefinedNoDataCellType(123)))

        def test_ushort(self):
            "encodes UShortArrayTile"
            self.roundTripWithNoDataCheck(UShortArrayTile.fill(45, 10, 15, UShortUserDefinedNoDataCellType(123)))

        def test_int(self):
            "encodes IntArrayTile"
            self.roundTripWithNoDataCheck(IntArrayTile.fill(45, 10, 15, IntUserDefinedNoDataCellType(123)))

        def test_float(self):
            "encodes FloatArrayTile"
            self.roundTripWithNoDataCheck(FloatArrayTile.fill(532.4, 10, 15, FloatUserDefinedNoDataCellType(2.2)))

        def test_double(self):
            "encodes DoubleArrayTile"
            self.roundTripWithNoDataCheck(DoubleArrayTile.fill(53232322.4, 10, 15, DoubleUserDefinedNoDataCellType(2.2)))

        def test_multiband(self):
            "encodes ArrayMultibandTile"
            tiles = [DoubleArrayTile.fill(53232322.4, 10, 15, DoubleUserDefinedNoDataCellType(42.23))] * 3
            thing = ArrayMultibandTile(tiles)
            self.roundTripWithNoDataCheck(thing)
