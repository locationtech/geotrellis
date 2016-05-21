from __future__ import absolute_import
from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from geotrellis.python.edited_avro.avro_builder import AvroSchemaBuilder
from geotrellis.raster.CellType import (
        ShortConstantNoDataCellType, ShortCellType, ShortUserDefinedNoDataCellType,
        UShortConstantNoDataCellType, UShortCellType, UShortUserDefinedNoDataCellType,
        IntConstantNoDataCellType, IntCellType, IntUserDefinedNoDataCellType,
        FloatConstantNoDataCellType, FloatCellType, FloatUserDefinedNoDataCellType,
        DoubleConstantNoDataCellType, DoubleCellType, DoubleUserDefinedNoDataCellType,
        ByteConstantNoDataCellType, ByteCellType, ByteUserDefinedNoDataCellType,
        UByteConstantNoDataCellType, UByteCellType, UByteUserDefinedNoDataCellType
        )
from geotrellis.raster.ShortArrayTile import ShortArrayTile
from geotrellis.raster.UShortArrayTile import UShortArrayTile
from geotrellis.raster.IntArrayTile import IntArrayTile
from geotrellis.raster.FloatArrayTile import FloatArrayTile
from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
from geotrellis.raster.ByteArrayTile import ByteArrayTile
from geotrellis.raster.UByteArrayTile import UByteArrayTile
from geotrellis.raster.BitArrayTile import BitArrayTile
from geotrellis.raster.MultibandTile import MultibandTile
from geotrellis.raster.ArrayMultibandTile import ArrayMultibandTile

from geotrellis.spark.io.avro.codecs.Implicits import tileUnionCodec

from geotrellis.raster.package_scala import SHORTMIN, BYTEMIN, isNoData, shortNODATA, ushortNODATA, NODATA, byteNODATA, ubyteNODATA

import avro.schema

import array

class ShortArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, ShortArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("ShortArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_int()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.array.tolist()
        if tile.cellType == ShortConstantNoDataCellType:
            dct["noDataValue"] = shortNODATA
        elif tile.cellType == ShortCellType:
            dct["noDataValue"] = None # TODO find matching java's null alternative
        elif isinstance(tile.cellType, ShortUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = array.array('h', dct["cells"])
        nodata = dct["noDataValue"]
        if nodata is None:
            cellType = ShortCellType
        elif nodata == SHORTMIN:
            cellType = ShortConstantNoDataCellType
        else:
            cellType = ShortUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return ShortArrayTile.applyStatic(arr, cols, rows, cellType)

class UShortArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, UShortArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("UShortArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_int()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.array.tolist()
        if tile.cellType == UShortConstantNoDataCellType:
            dct["noDataValue"] = ushortNODATA
        elif tile.cellType == UShortCellType:
            dct["noDataValue"] = None
        elif isinstance(tile.cellType, UShortUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = array.array('H', dct["cells"])
        nodata = dct["noDataValue"]
        if nodata is None:
            cellType = UShortCellType
        elif nodata == 0:
            cellType = UShortConstantNoDataCellType
        else:
            cellType = UShortUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return UShortArrayTile.applyStatic(arr, cols, rows, cellType)

class IntArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, IntArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("IntArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_int()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.array.tolist()
        if tile.cellType == IntConstantNoDataCellType:
            dct["noDataValue"] = NODATA
        elif tile.cellType == IntCellType:
            dct["noDataValue"] = None
        elif isinstance(tile.cellType, IntUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        #arr = array.array('i', dct["cells"])
        # <debug>
        cells = dct["cells"]
        print("dct.cells: {c}".format(c=cells))
        arr = array.array('i', cells)
        #print("array.array: {arr}".format(arr=arr))
        #print("arr.arr.lst: {arr}".format(arr=arr.tolist()))
        # </debug>
        nodata = dct["noDataValue"]
        if nodata is None:
            cellType = IntCellType
        elif isNoData(nodata):
            cellType = IntConstantNoDataCellType
        else:
            cellType = IntUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return IntArrayTile.applyStatic(arr, cols, rows, cellType)

class FloatArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, FloatArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("FloatArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_float()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_float(),
                    _.create_boolean()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.array.tolist()
        if tile.cellType == FloatConstantNoDataCellType:
            dct["noDataValue"] = True
        elif tile.cellType == FloatCellType:
            dct["noDataValue"] = False
        elif isinstance(tile.cellType, FloatUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected. {tp}".format(ct = tile.cellType, tp = type(tile.cellType)))

    def decode(self, dct):
        arr = array.array('f', dct["cells"])
        nodata = dct["noDataValue"]
        if nodata is True:
            cellType = FloatConstantNoDataCellType
        elif nodata is False:
            cellType = FloatCellType
        else:
            cellType = FloatUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return FloatArrayTile.applyStatic(arr, cols, rows, cellType)

class DoubleArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, DoubleArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("DoubleArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_double()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_double(),
                    _.create_boolean()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.array.tolist()
        if tile.cellType == DoubleConstantNoDataCellType:
            dct["noDataValue"] = True
        elif tile.cellType == DoubleCellType:
            dct["noDataValue"] = False
        elif isinstance(tile.cellType, DoubleUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = array.array('d', dct["cells"])
        nodata = dct["noDataValue"]
        if nodata is True:
            cellType = DoubleConstantNoDataCellType
        elif nodata is False:
            cellType = DoubleCellType
        else:
            cellType = DoubleUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return DoubleArrayTile.applyStatic(arr, cols, rows, cellType)

class ByteArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, ByteArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("ByteArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells", _.create_bytes())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        #dct["cells"] = tile.array.tolist() # TODO wrap if needed
        dct["cells"] = tile.array.tostring()
        if tile.cellType == ByteConstantNoDataCellType:
            dct["noDataValue"] = byteNODATA
        elif tile.cellType == ByteCellType:
            dct["noDataValue"] = None
        elif isinstance(tile.cellType, ByteUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = array.array('b')
        arr.fromstring(dct["cells"])
        nodata = dct["noDataValue"]
        if nodata is None:
            cellType = ByteCellType
        elif nodata == BYTEMIN:
            cellType = ByteConstantNoDataCellType
        else:
            cellType = ByteUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return ByteArrayTile.applyStatic(arr, cols, rows, cellType)

class UByteArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, UByteArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("UByteArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells", _.create_bytes())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        #dct["cells"] = tile.array.tolist() # TODO wrap if needed
        dct["cells"] = tile.array.tostring()
        if tile.cellType == UByteConstantNoDataCellType:
            dct["noDataValue"] = ubyteNODATA
        elif tile.cellType == UByteCellType:
            dct["noDataValue"] = None
        elif isinstance(tile.cellType, UByteUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = array.array('B')
        arr.fromstring(dct["cells"])
        nodata = dct["noDataValue"]
        if nodata is None:
            cellType = UByteCellType
        elif nodata == 0:
            cellType = UByteConstantNoDataCellType
        else:
            cellType = UByteUserDefinedNoDataCellType(nodata)

        cols = dct["cols"]
        rows = dct["rows"]
        return UByteArrayTile.applyStatic(arr, cols, rows, cellType)

class BitArrayTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, BitArrayTile)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("BitArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells", _.create_bytes())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.array.tolist() # TODO wrap if needed

    def decode(self, dct):
        arr = array.array('b', dct["cells"])
        cols = dct["cols"]
        rows = dct["rows"]
        return BitArrayTile.applyStatic(arr, cols, rows)

class MultibandTileCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, MultibandTile)
        self._tileUnionCodec = tileUnionCodec()

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("ArrayMultibandTile", namespace = "geotrellis.raster")
        _.add_field("bands", _.begin_array(
            #_.begin_with_schema_json(self._tileUnionCodec.schema.to_json())
            self._tileUnionCodec.schema.to_json()
            ).end())
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tile, dct):
        bands = [tile.band(i) for i in xrange(0, tile.bandCount)]
        dct["bands"] = map(self._tileUnionCodec.encode, bands)

    def decode(self, dct):
        bands = map(self._tileUnionCodec.decode, dct["bands"])
        return ArrayMultibandTile(bands)
