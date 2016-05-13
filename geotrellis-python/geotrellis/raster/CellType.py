from geotrellis.python.util.utils import float_eq
import re
import math

class DataType(object):

    @property
    def bytes(self):
        return self.bits / 8

    def union(self, other):
        if self.bits < other.bits:
            return other
        elif self.bits > other.bits:
            return self
        elif self.isFloatingPoint and not other.isFloatingPoint:
            return self
        else:
            return other

    def intersect(self, other):
        if self.bits < other.bits:
            return self
        elif self.bits > other.bits:
            return other
        elif self.isFloatingPoint and not other.isFloatingPoint:
            return other
        else:
            return self

    def contains(self, other):
        return self.bits >= other.bits

    def numBytes(self, size):
        return self.bytes * size

    def __str__(self):
        return self.name

class BitCells(DataType):
    @property
    def bits(self):
        return 1

    @property
    def isFloatingPoint(self):
        return False

    @property
    def name(self):
        return "bool"

class ByteCells(DataType):
    @property
    def bits(self):
        return 8

    @property
    def isFloatingPoint(self):
        return False

    @property
    def name(self):
        return "int8"

class UByteCells(DataType):
    @property
    def bits(self):
        return 8

    @property
    def isFloatingPoint(self):
        return False

    @property
    def name(self):
        return "uint8"

class ShortCells(DataType):
    @property
    def bits(self):
        return 16

    @property
    def isFloatingPoint(self):
        return False

    @property
    def name(self):
        return "int16"

class UShortCells(DataType):
    @property
    def bits(self):
        return 16

    @property
    def isFloatingPoint(self):
        return False

    @property
    def name(self):
        return "uint16"

class IntCells(DataType):
    @property
    def bits(self):
        return 32

    @property
    def isFloatingPoint(self):
        return False

    @property
    def name(self):
        return "int32"

class FloatCells(DataType):
    @property
    def bits(self):
        return 32

    @property
    def isFloatingPoint(self):
        return True

    @property
    def name(self):
        return "float32"

class DoubleCells(DataType):
    @property
    def bits(self):
        return 64

    @property
    def isFloatingPoint(self):
        return True

    @property
    def name(self):
        return "float64"

class NoDataHandling(object):
    pass

class ConstantNoData(NoDataHandling):
    pass

class NoNoData(NoDataHandling):
    def __str__(self):
        return self.name + "raw"

class UserDefinedNoData(NoDataHandling):
    @property
    def noDataValue(self):
        return self._noDataValue

    def __str__(self):
        return self.name + "ud" + repr(self.noDataValue)
    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return False
        return self.noDataValue == other.noDataValue
    def __hash__(self):
        return hash(self.noDataValue)

class _BitCellTypeTemp(BitCells, NoNoData):
    def numBytes(self, size):
        return (size + 7) / 8

BitCellType = _BitCellTypeTemp()

ByteCellType = type("ByteCellType", (NoNoData, ByteCells), {})()
ByteConstantNoDataCellType = type(
    "ByteConstantNoDataCellType",
    (ConstantNoData, ByteCells),
    {})()
class ByteUserDefinedNoDataCellType(UserDefinedNoData, ByteCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue

UByteCellType = type("UByteCellType", (NoNoData, UByteCells), {})()
UByteConstantNoDataCellType = type(
    "UByteConstantNoDataCellType",
    (ConstantNoData, UByteCells),
    {})()
class UByteUserDefinedNoDataCellType(UserDefinedNoData, UByteCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue

ShortCellType = type("ShortCellType", (NoNoData, ShortCells), {})()
ShortConstantNoDataCellType = type(
    "ShortConstantNoDataCellType",
    (ConstantNoData, ShortCells),
    {})()
class ShortUserDefinedNoDataCellType(UserDefinedNoData, ShortCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue

UShortCellType = type("UShortCellType", (NoNoData, UShortCells), {})()
UShortConstantNoDataCellType = type(
    "UShortConstantNoDataCellType",
    (ConstantNoData, UShortCells),
    {})()
class UShortUserDefinedNoDataCellType(UserDefinedNoData, UShortCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue

IntCellType = type("IntCellType", (NoNoData, IntCells), {})()
IntConstantNoDataCellType = type(
    "IntConstantNoDataCellType",
    (ConstantNoData, IntCells),
    {})()
class IntUserDefinedNoDataCellType(UserDefinedNoData, IntCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue

FloatCellType = type("FloatCellType", (NoNoData, FloatCells), {})()
FloatConstantNoDataCellType = type(
    "FloatConstantNoDataCellType",
    (ConstantNoData, FloatCells),
    {})()
class FloatUserDefinedNoDataCellType(UserDefinedNoData, FloatCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue
    def __eq__(self, other):
        if not isinstance(other, FloatUserDefinedNoDataCellType):
            return False
        return float_eq(self.noDataValue, other.noDataValue)

DoubleCellType = type("DoubleCellType", (NoNoData, DoubleCells), {})()
DoubleConstantNoDataCellType = type(
    "DoubleConstantNoDataCellType",
    (ConstantNoData, DoubleCells),
    {})()
class DoubleUserDefinedNoDataCellType(UserDefinedNoData, DoubleCells):
    def __init__(self, noDataValue):
        self._noDataValue = noDataValue
    def __eq__(self, other):
        if not isinstance(other, DoubleUserDefinedNoDataCellType):
            return False
        return float_eq(self.noDataValue, other.noDataValue)

TYPE_BYTE = 0
TYPE_SHORT = 2
TYPE_INT = 3
TYPE_FLOAT = 4
TYPE_DOUBLE = 5

class CellType(object):
    @staticmethod
    def fromAwtType(awtType):
        if awtType == TYPE_BYTE:
            return ByteConstantNoDataCellType
        elif awtType == TYPE_SHORT:
            return ShortConstantNoDataCellType
        elif awtType == TYPE_INT:
            return IntConstantNoDataCellType
        elif awtType == TYPE_FLOAT:
            return FloatConstantNoDataCellType
        elif awtType == TYPE_DOUBLE:
            return DoubleConstantNoDataCellType
        else:
            raise Exception("AWT type {awtType} is not supported".format(awtType = awtType))

    @staticmethod
    def toAwtType(cellType):
        if isinstance(cellType, BitCells):
            return TYPE_BYTE
        elif isinstance(cellType, ByteCells):
            return TYPE_BYTE
        elif isinstance(cellType, UByteCells):
            return TYPE_BYTE
        elif isinstance(cellType, ShortCells):
            return TYPE_SHORT
        elif isinstance(cellType, UShortCells):
            return TYPE_SHORT
        elif isinstance(cellType, IntCells):
            return TYPE_INT
        elif isinstance(cellType, FloatCells):
            return TYPE_FLOAT
        elif isinstance(cellType, DoubleCells):
            return TYPE_DOUBLE

    @staticmethod
    def fromString(name):
        if name == "bool" or name == "boolraw":
            return BitCellType
        elif name == "int8raw":
            return ByteCellType
        elif name == "uint8raw":
            return UByteCellType
        elif name == "int16raw":
            return ShortCellType
        elif name == "uint16raw":
            return UShortCellType
        elif name == "float32raw":
            return FloatCellType
        elif name == "float64raw":
            return DoubleCellType
        elif name == "int8":
            return ByteConstantNoDataCellType
        elif name == "uint8":
            return UByteConstantNoDataCellType
        elif name == "int16":
            return ShortConstantNoDataCellType
        elif name == "uint16":
            return UShortConstantNoDataCellType
        elif name == "int32":
            return IntConstantNoDataCellType
        elif name == "float32":
            return FloatConstantNoDataCellType
        elif name == "float64":
            return DoubleConstantNoDataCellType
        elif name.startswith("int8ud"):
            ndVal = _findFirst("\\d+$", name)
            if ndVal is None:
                raise Exception("Cell type {name} is not supported".format(name = name))
            return ByteUserDefinedNoDataCellType(int(ndVal))
        elif name.startswith("uint8ud"):
            ndVal = _findFirst("\\d+$", name)
            if ndVal is None:
                raise Exception("Cell type {name} is not supported".format(name = name))
            UByteUserDefinedNoDataCellType(int(ndVal))
        elif name.startswith("int16ud"):
            ndVal = _findFirst("\\d+$", name)
            if ndVal is None:
                raise Exception("Cell type {name} is not supported".format(name = name))
            ShortUserDefinedNoDataCellType(int(ndVal))
        elif name.startswith("uint16ud"):
            ndVal = _findFirst("\\d+$", name)
            if ndVal is None:
                raise Exception("Cell type {name} is not supported".format(name = name))
            UShortUserDefinedNoDataCellType(int(ndVal))
        elif name.startswith("int32ud"):
            ndVal = _findFirst("\\d+$", name)
            if ndVal is None:
                raise Exception("Cell type {name} is not supported".format(name = name))
            IntUserDefinedNoDataCellType(int(ndVal))
        elif name.startswith("float32ud"):
            string = name[len("float32ud"):]
            try:
                ndVal = float(string)
            except ValueError:
                raise Exception("Cell type {name} is not supported".format(name = name))
            if math.isnan(ndVal):
                return FloatConstantNoDataCellType
            else:
                return FloatUserDefinedNoDataCellType(ndVal)
        elif name.startswith("float64ud"):
            string = name[len("float64ud"):]
            try:
                ndVal = float(string)
            except ValueError:
                raise Exception("Cell type {name} is not supported".format(name = name))
            if math.isnan(ndVal):
                return DoubleConstantNoDataCellType
            else:
                return DoubleUserDefinedNoDataCellType(ndVal)
        else:
            raise Exception("Cell type {name} is not supported".format(name = name))

def _findFirst(pattern, string):
    it = re.finditer(pattern, string)
    try:
        return it.next().group()
    except StopIteration:
        return None
