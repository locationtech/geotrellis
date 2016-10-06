from __future__ import absolute_import
from tests.geotrellis_test.spark.io.avro.AvroTools import AvroTools
from geotrellis.raster.CellType import (
        ShortCellType,
        ShortUserDefinedNoDataCellType,
        UShortCellType,
        UShortUserDefinedNoDataCellType,
        IntCellType,
        IntUserDefinedNoDataCellType,
        FloatCellType,
        FloatUserDefinedNoDataCellType,
        DoubleCellType,
        DoubleUserDefinedNoDataCellType,
        ByteCellType,
        ByteUserDefinedNoDataCellType,
        UByteCellType,
        UByteUserDefinedNoDataCellType
        )
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
from nose import tools

@tools.istest
class TileCodecsSpec(AvroTools, Spec):
    @tools.istest
    class inner_tile_codecs(object):
        "TileCodecs"

        @tools.istest
        def test_byte(self):
            "encodes ByteArrayTile"
            self.roundTripWithNoDataCheck(ByteArrayTile.fill(127, 10, 15))

        @tools.istest
        def test_ubyte(self):
            "encodes UByteArrayTile"
            self.roundTripWithNoDataCheck(UByteArrayTile.fill(127, 10, 15))

        @tools.istest
        def test_short(self):
            "encodes ShortArrayTile"
            self.roundTripWithNoDataCheck(ShortArrayTile.fill(45, 10, 15))

        @tools.istest
        def test_ushort(self):
            "encodes UShortArrayTile"
            self.roundTripWithNoDataCheck(UShortArrayTile.fill(45, 10, 15))

        @tools.istest
        def test_int(self):
            "encodes IntArrayTile"
            self.roundTripWithNoDataCheck(IntArrayTile.fill(45, 10, 15))

        @tools.istest
        def test_float(self):
            "encodes FloatArrayTile"
            self.roundTripWithNoDataCheck(FloatArrayTile.fill(532.4, 10, 15))

        @tools.istest
        def test_double(self):
            "encodes DoubleArrayTile"
            self.roundTripWithNoDataCheck(DoubleArrayTile.fill(53232322.4, 10, 15))

        @tools.istest
        def test_multiband(self):
            "encodes ArrayMultibandTile"
            tiles = [DoubleArrayTile.fill(53232322.4, 10, 15)] * 3
            thing = ArrayMultibandTile(tiles)
            self.roundTripWithNoDataCheck(thing)

    @tools.istest
    class inner_no_nodata_tile_codecs(object):
        "No NoData TileCodecs"

        @tools.istest
        def test_byte(self):
            "encodes ByteArrayTile"
            self.roundTripWithNoDataCheck(ByteArrayTile.fill(127, 10, 15, ByteCellType))

        @tools.istest
        def test_ubyte(self):
            "encodes UByteArrayTile"
            self.roundTripWithNoDataCheck(UByteArrayTile.fill(127, 10, 15, UByteCellType))

        @tools.istest
        def test_short(self):
            "encodes ShortArrayTile"
            self.roundTripWithNoDataCheck(ShortArrayTile.fill(45, 10, 15, ShortCellType))

        @tools.istest
        def test_ushort(self):
            "encodes UShortArrayTile"
            self.roundTripWithNoDataCheck(UShortArrayTile.fill(45, 10, 15, UShortCellType))

        @tools.istest
        def test_int(self):
            "encodes IntArrayTile"
            self.roundTripWithNoDataCheck(IntArrayTile.fill(45, 10, 15, IntCellType))

        @tools.istest
        def test_float(self):
            "encodes FloatArrayTile"
            self.roundTripWithNoDataCheck(FloatArrayTile.fill(532.4, 10, 15, FloatCellType))

        @tools.istest
        def test_double(self):
            "encodes DoubleArrayTile"
            self.roundTripWithNoDataCheck(DoubleArrayTile.fill(53232322.4, 10, 15, DoubleCellType))

        @tools.istest
        def test_multiband(self):
            "encodes ArrayMultibandTile"
            tiles = [DoubleArrayTile.fill(53232322.4, 10, 15, DoubleCellType)] * 3
            thing = ArrayMultibandTile(tiles)
            self.roundTripWithNoDataCheck(thing)

    @tools.istest
    class inner_user_defined_tile_codecs(object):
        "UserDefined TileCodecs"

        @tools.istest
        def test_byte(self):
            "encodes ByteArrayTile"
            self.roundTripWithNoDataCheck(ByteArrayTile.fill(127, 10, 15, ByteUserDefinedNoDataCellType(123)))

        @tools.istest
        def test_ubyte(self):
            "encodes UByteArrayTile"
            self.roundTripWithNoDataCheck(UByteArrayTile.fill(127, 10, 15, UByteUserDefinedNoDataCellType(123)))

        @tools.istest
        def test_short(self):
            "encodes ShortArrayTile"
            self.roundTripWithNoDataCheck(ShortArrayTile.fill(45, 10, 15, ShortUserDefinedNoDataCellType(123)))

        @tools.istest
        def test_ushort(self):
            "encodes UShortArrayTile"
            self.roundTripWithNoDataCheck(UShortArrayTile.fill(45, 10, 15, UShortUserDefinedNoDataCellType(123)))

        @tools.istest
        def test_int(self):
            "encodes IntArrayTile"
            self.roundTripWithNoDataCheck(IntArrayTile.fill(45, 10, 15, IntUserDefinedNoDataCellType(123)))

        @tools.istest
        def test_float(self):
            "encodes FloatArrayTile"
            self.roundTripWithNoDataCheck(FloatArrayTile.fill(532.4, 10, 15, FloatUserDefinedNoDataCellType(2.2)))

        @tools.istest
        def test_double(self):
            "encodes DoubleArrayTile"
            self.roundTripWithNoDataCheck(DoubleArrayTile.fill(53232322.4, 10, 15, DoubleUserDefinedNoDataCellType(2.2)))

        @tools.istest
        def test_multiband(self):
            "encodes ArrayMultibandTile"
            tiles = [DoubleArrayTile.fill(53232322.4, 10, 15, DoubleUserDefinedNoDataCellType(42.23))] * 3
            thing = ArrayMultibandTile(tiles)
            self.roundTripWithNoDataCheck(thing)
