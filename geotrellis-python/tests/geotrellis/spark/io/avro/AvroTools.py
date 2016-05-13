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
from geotrellis.raster.package_scala import SHORTMIN, BYTEMIN, isNoData, NODATA, byteNODATA, ubyteNODATA
from geotrellis.spark.io.avro.AvroEncoder import AvroEncoder
from geotrellis.python.util.utils import float_eq
import json

from nose import tools

@tools.nottest
class AvroTools(object):
    def roundTrip(self, thing, codec = None):
        T = type(thing)
        bytesarray = AvroEncoder.toBinary(thing, codec)
        fromBytes = AvroEncoder.fromBinary(T, bytesarray, codec=codec)
        assert fromBytes == thing
        jason = AvroEncoder.toJson(thing, codec)
        fromJson = AvroEncoder.fromJson(T, jason, codec=codec)
        assert fromJson == thing

    def roundTripWithNoDataCheck(self, thing, codec = None):
        T = type(thing)
        bytesarray = AvroEncoder.toBinary(thing, codec)
        fromBytes = AvroEncoder.fromBinary(T, bytesarray, codec=codec)
        assert fromBytes == thing
        jason = AvroEncoder.toJson(thing, codec)
        _checkNoData(thing, jason)
        fromJson = AvroEncoder.fromJson(T, jason, codec=codec)
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

@tools.nottest
class NoDataChecker(object):
    def checkNoData(self, jason):
        dct = json.loads(jason)
        nodata = self.extractNoData(dct)
        self.doCheck(nodata)
    def extractNoData(self, dct):
        if dct.has_key(_noDataAttrName):
            nodata = dct[_noDataAttrName]  
            return (nodata,)
        else:
            return None
    def doCheck(self, nodata):
        pass

@tools.nottest
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

@tools.nottest
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

@tools.nottest
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

@tools.nottest
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
            #assert nodata == ({"float": cellType.noDataValue},)
            result = (len(nodata) == 1 and
                    nodata[0].keys() == ["float"] and
                    float_eq(nodata[0]["float"], cellType.noDataValue))
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

@tools.nottest
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
            #assert nodata == ({"double": cellType.noDataValue},)
            result = (len(nodata) == 1 and
                    nodata[0].keys() == ["double"] and
                    float_eq(nodata[0]["double"], cellType.noDataValue))
        else:
            raise Exception(
                    "The cellType {ct} was not expected.".format(ct=cellType))

@tools.nottest
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

@tools.nottest
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

@tools.nottest
class BitNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def doCheck(self, nodata):
        assert nodata is None

@tools.nottest
class MultibandNoDataChecker(NoDataChecker):
    def __init__(self, cellType):
        self.cellType = cellType

    def checkNoData(self, jason):
        def performCheck(checkerFunc):
            dct = json.loads(jason)
            for bandWrapper in dct["bands"]:
                band = bandWrapper[bandWrapper.keys()[0]]
                nodata = self.extractNoData(band)
                checkerFunc(nodata)
        cellType = self.cellType
        if isinstance(cellType, BitCells):
            performCheck(BitNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, ShortCells):
            performCheck(ShortNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, UShortCells):
            performCheck(UShortNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, IntCells):
            performCheck(IntNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, FloatCells):
            performCheck(FloatNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, DoubleCells):
            performCheck(DoubleNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, ByteCells):
            performCheck(ByteNoDataChecker(cellType).doCheck)
        elif isinstance(cellType, UByteCells):
            performCheck(UByteNoDataChecker(cellType).doCheck)
        else:
            raise Exception(
                    "cellType {ct} was not expected.".format(ct = cellType))
