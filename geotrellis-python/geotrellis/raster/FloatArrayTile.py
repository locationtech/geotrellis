from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedFloatNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import FloatCellType, FloatConstantNoDataCellType, FloatUserDefinedNoDataCellType
from geotrellis.raster.package_scala import f2i, i2f, f2d, d2f

import array
import struct

class FloatArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('f', arr)
        else:
            self.array = arr
        self._cols = cols
        self._rows = rows

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    @property
    def cellType(self):
        return self._cellType

    def toBytes(self):
        length = len(self.array)
        typecode = "{len}f".format(len = length)
        arr = array.array('b')
        arr.fromstring(struct.pack(typecode, *self.array.tolist()))
        return arr

    def copy(self):
        arr = array.array('f', self.array.tolist())
        return ArrayTile.applyStatic(arr, self.cols, self.rows)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = FloatConstantNoDataCellType):
        if cellType is FloatCellType:
            return FloatRawArrayTile(arr, cols, rows)
        elif cellType is FloatConstantNoDataCellType:
            return FloatConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, FloatUserDefinedNoDataCellType):
            return FloatUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = FloatConstantNoDataCellType):
        arr = array.array('f', [0.0] * (cols*rows))
        if cellType is FloatCellType:
            return FloatRawArrayTile(arr, cols, rows)
        elif cellType is FloatConstantNoDataCellType:
            return FloatConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, FloatUserDefinedNoDataCellType):
            return FloatUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = FloatConstantNoDataCellType):
        if cellType is FloatCellType:
            return FloatArrayTile.ofDim(cols, rows, cellType)
        elif cellType is FloatConstantNoDataCellType:
            return FloatArrayTile.fill(float("nan"), cols, rows, cellType)
        elif isinstance(cellType, FloatUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return FloatArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = FloatConstantNoDataCellType):
        arr = array.array('f', [v] * (cols*rows))
        if cellType is FloatCellType:
            return FloatRawArrayTile(arr, cols, rows)
        elif cellType is FloatConstantNoDataCellType:
            return FloatConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, FloatUserDefinedNoDataCellType):
            return FloatUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = FloatConstantNoDataCellType):
        arr = array.array('f')
        length = len(bytesarray)
        typecode = "{len}b".format(len = length)
        arr.fromstring(struct.pack(typecode, *bytesarray.tolist()))

        if cellType is FloatCellType:
            return FloatRawArrayTile(arr, cols, rows)
        elif cellType is FloatConstantNoDataCellType:
            return FloatConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, FloatUserDefinedNoDataCellType):
            return FloatUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import FloatArrayTileCodec
    return FloatArrayTileCodec()

class FloatRawArrayTile(FloatArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        FloatArrayTile.__init__(self, arr, cols, rows)
        self._cellType = FloatCellType

    def __eq__(self, other):
        if not isinstance(other, FloatRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return int(self.array[i])

    def applyDouble(self, i):
        return self.array[i]

    def _update(self, i, z):
        self.array[i] = float(z)

    def updateDouble(self, i, z):
        self.array[i] = z

class FloatConstantNoDataArrayTile(FloatArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows):
        FloatArrayTile.__init__(self, arr, cols, rows)
        self._cellType = FloatConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, FloatConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return f2i(self.array[i])

    def applyDouble(self, i):
        return f2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = i2f(z)

    def updateDouble(self, i, z):
        self.array[i] = d2f(z)

class FloatUserDefinedNoDataArrayTile(UserDefinedFloatNoDataConversions, FloatArrayTile):
    implicits = {"AvroRecordCodec": generateCodec}

    def __init__(self, arr, cols, rows, cellType):
        FloatArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, FloatUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.udf2i(self.array[i])

    def applyDouble(self, i):
        return self.udf2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2udf(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2udf(z)
