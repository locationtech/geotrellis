from geotrellis.raster.CellType import (
        BitCells,
        ShortCells, ShortConstantNoDataCellType, ShortCellType,
        ShortUserDefinedNoDataCellType,
        UShortCells, UShortConstantNoDataCellType, UShortCellType,
        UShortUserDefinedNoDataCellType,
        IntCells, IntConstantNoDataCellType, IntCellType,
        IntUserDefinedNoDataCellType,
        FloatCells, FloatConstantNoDataCellType, FloatCellType,
        FloatUserDefinedNoDataCellType,
        DoubleCells, DoubleConstantNoDataCellType, DoubleCellType,
        DoubleUserDefinedNoDataCellType,
        ByteCells, ByteConstantNoDataCellType, ByteCellType,
        ByteUserDefinedNoDataCellType,
        UByteCells, UByteConstantNoDataCellType, UByteCellType,
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
from geotrellis.raster.MultibandTile import MultibandTile
from geotrellis.raster.package_scala import SHORTMIN, BYTEMIN, isNoData
from geotrellis.spark.io.avro.AvroEncoder import AvroEncoder
import json

class AvroTools(object):
    def roundTrip(self, thing, codec = None):
        T = type(thing)
        bytesarray = AvroEncoder.toBinary(thing, codec)
        fromBytes = AvroEncoder.fromBinary(T, bytesarray, codec)
        assert fromBytes == thing
        jason = AvroEncoder.toJson(thing, codec)
        fromJson = AvroEncoder.fromJson(T, jason, codec)
        assert fromJson == thing

    def roundTripWithNoDataCheck(self, thing, codec = None):
        T = type(thing)
        bytesarray = AvroEncoder.toBinary(thing, codec)
        fromBytes = AvroEncoder.fromBinary(T, bytesarray, codec)
        assert fromBytes == thing
        jason = AvroEncoder.toJson(thing, codec)
        _checkNoData(thing, jason)
        fromJson = AvroEncoder.fromJson(T, jason, codec)
        assert fromJson == thing

def _checkNoData(thing, jason):
    nodataChecker = _nodataChecker(thing)
    nodataChecker.checkNoData(jason)

def _nodataChecker(thing):
    if isinstance(thing, ShortArrayTile):
        return ShortNoDataChecker(thing.cellType)
    elif isinstance(thing, UShortArrayTile):
        return UShortNoDataChecker(thing.cellType)
    elif isinstance(thing, IntArrayTile):
        return IntNoDataChecker(thing.cellType)
    elif isinstance(thing, FloatArrayTile):
        return FloatNoDataChecker(thing.cellType)
    elif isinstance(thing, DoubleArrayTile):
        return DoubleNoDataChecker(thing.cellType)
    elif isinstance(thing, ByteArrayTile):
        return ByteNoDataChecker(thing.cellType)
    elif isinstance(thing, UByteArrayTile):
        return UByteNoDataChecker(thing.cellType)
    elif isinstance(thing, BitArrayTile):
        return BitNoDataChecker(thing.cellType)
    elif isinstance(thing, MultibandTile):
        return MultibandNoDataChecker(thing.cellType)
    else:
        raise Exception(
                "No NoDataChecker was found for tile type {tp}".format(
                    tp = type(thing)))

_noDataAttrName = "noDataValue"

class NoDataChecker(object):
    def checkNoData(self, jason):
        nodata = self.extractNoData(jason)
        self.doCheck(nodata)
    def extractNoData(self, jason):
        dct = json.loads(jason)
        if dct.has_key(_noDataAttrName):
            nodata = dct[_noDataAttrName]  
            return (nodata,)
        else:
            return None
    def doCheck(self, nodata):
        pass

class ShortNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is ShortConstantNoDataCellType:
            assert nodata == ({"int": SHORTMIN},)
        elif cellType is ShortCellType:
            assert nodata == (None,)
        elif isinstance(cellType, ShortUserDefinedNoDataCellType):
            assert nodata == ({"int": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class UShortNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is UShortConstantNoDataCellType:
            assert nodata == ({"int": 0},)
        elif cellType is UShortCellType:
            assert nodata == (None,)
        elif isinstance(cellType, UShortUserDefinedNoDataCellType):
            assert nodata == ({"int": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class IntNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is IntConstantNoDataCellType:
            assert nodata == ({"int": NODATA},)
        elif cellType is IntCellType:
            assert nodata == (None,)
        elif isinstance(cellType, IntUserDefinedNoDataCellType):
            assert nodata == ({"int": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class FloatNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is FloatConstantNoDataCellType:
            assert nodata == ({"boolean": True},)
        elif cellType is FloatCellType:
            assert nodata == ({"boolean": False},)
        elif isinstance(cellType, FloatUserDefinedNoDataCellType):
            assert nodata == ({"float": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class DoubleNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is DoubleConstantNoDataCellType:
            assert nodata == ({"boolean": True},)
        elif cellType is DoubleCellType:
            assert nodata == ({"boolean": False},)
        elif isinstance(cellType, DoubleUserDefinedNoDataCellType):
            assert nodata == ({"double": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class ByteNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is ByteConstantNoDataCellType:
            assert nodata == ({"int": byteNODATA},)
        elif cellType is ByteCellType:
            assert nodata == (None,)
        elif isinstance(cellType, ByteUserDefinedNoDataCellType):
            assert nodata == ({"int": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class UByteNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        cellType = self.cellType
        if cellType is UByteConstantNoDataCellType:
            assert nodata == ({"int": ubyteNODATA},)
        elif cellType is UByteCellType:
            assert nodata == (None,)
        elif isinstance(cellType, UByteUserDefinedNoDataCellType):
            assert nodata == ({"int": cellType.noDataValue},)
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

class BitNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        assert nodata is None

class MultibandNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def checkNoData(self, jason):
        cellType = self.cellType
        if isinstance(cellType, BitCells):
            BitNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, ShortCells):
            ShortNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, UShortCells):
            UShortNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, IntCells):
            IntNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, FloatCells):
            FloatNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, DoubleCells):
            DoubleNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, ByteCells):
            ByteNoDataChecker(cellType).checkNoData(jason)
        elif isinstance(cellType, UByteCells):
            UByteNoDataChecker(cellType).checkNoData(jason)
        else:
            raise Exception(
                    "cellType {ct} was not expected.".format(ct = cellType))
