from geotrellis.raster.UserDefinedNoDataConversions import UserDefinedNoDataConversions
from geotrellis.raster.MutableArrayTile import MutableArrayTile
from geotrellis.raster.CellType import ByteCellType, ByteConstantNoDataCellType
from geotrellis.raster.package_scala import intToByte, b2i, b2d, i2b, d2b, byteNODATA

import array

class ByteArrayTile(MutableArrayTile):
    def __init__(self, arr, cols, rows):
        if isinstance(arr, list):
            self.array = array.array('b', arr)
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
        return array.array('b', self.array.tolist())

    def copy(self):
        arr = array.array('b', self.array.tolist())
        return ArrayTile.applyStatic(arr, self.cols, self.rows)

    @staticmethod
    def applyStatic(arr, cols, rows, cellType = ByteConstantNoDataCellType):
        if cellType is ByteCellType:
            return ByteRawArrayTile(arr, cols, rows)
        elif cellType is ByteConstantNoDataCellType:
            return ByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ByteUserDefinedNoDataCellType):
            return ByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def ofDim(cols, rows, cellType = ByteConstantNoDataCellType):
        arr = array.array('b', [0] * (cols*rows))
        if cellType is ByteCellType:
            return ByteRawArrayTile(arr, cols, rows)
        elif cellType is ByteConstantNoDataCellType:
            return ByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ByteUserDefinedNoDataCellType):
            return ByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def empty(cols, rows, cellType = ByteConstantNoDataCellType):
        if cellType is ByteCellType:
            return ByteArrayTile.ofDim(cols, rows, cellType)
        elif cellType is ByteConstantNoDataCellType:
            return ByteArrayTile.fill(byteNODATA, cols, rows, cellType)
        elif isinstance(cellType, ByteUserDefinedNoDataCellType):
            nd = cellType.noDataValue
            return ByteArrayTile.fill(nd, cols, rows, cellType)

    @staticmethod
    def fill(v, cols, rows, cellType = ByteConstantNoDataCellType):
        arr = array.array('b', [v] * (cols*rows))
        if cellType is ByteCellType:
            return ByteRawArrayTile(arr, cols, rows)
        elif cellType is ByteConstantNoDataCellType:
            return ByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ByteUserDefinedNoDataCellType):
            return ByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

    @staticmethod
    def fromBytes(bytesarray, cols, rows, cellType = ByteConstantNoDataCellType):
        arr = array.array('b', bytesarray.tolist())
        if cellType is ByteCellType:
            return ByteRawArrayTile(arr, cols, rows)
        elif cellType is ByteConstantNoDataCellType:
            return ByteConstantNoDataArrayTile(arr, cols, rows)
        elif isinstance(cellType, ByteUserDefinedNoDataCellType):
            return ByteUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

def ByteRawArrayTile(ByteArrayTile):
    def __init__(self, arr, cols, rows):
        ByteArrayTile.__init__(self, arr, cols, rows)
        self._cellType = ByteCellType

    def __eq__(self, other):
        if not isinstance(other, ByteRawArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return self.array[i]

    def applyDouble(self, i):
        return float(self.array[i])

    def _update(self, i, z):
        self.array[i] = intToByte(z)

    def updateDouble(self, i, z):
        self.array[i] = intToByte(int(z))

def ByteConstantNoDataArrayTile(ByteArrayTile):
    def __init__(self, arr, cols, rows):
        ByteArrayTile.__init__(self, arr, cols, rows)
        self._cellType = ByteConstantNoDataCellType

    def __eq__(self, other):
        if not isinstance(other, ByteConstantNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows))

    def apply(self, i):
        return b2i(self.array[i])

    def applyDouble(self, i):
        return b2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = i2b(z)

    def updateDouble(self, i, z):
        self.array[i] = d2b(z)

class ByteUserDefinedNoDataArrayTile(UserDefinedByteNoDataConversions, ByteArrayTile):
    def __init__(self, arr, cols, rows, cellType):
        ByteArrayTile.__init__(self, arr, cols, rows)
        self._cellType = cellType

    def __eq__(self, other):
        if not isinstance(other, ByteUserDefinedNoDataArrayTile):
            return False
        return (self.array == other.array and
                self.cols == other.cols and
                self.rows == other.rows and
                self.cellType == other.cellType)
    
    def __hash__(self):
        return hash((tuple(self.array), self.cols, self.rows, self.cellType))

    def apply(self, i):
        return self.udb2i(self.array[i])

    def applyDouble(self, i):
        return self.udb2d(self.array[i])

    def _update(self, i, z):
        self.array[i] = self.i2udb(z)

    def updateDouble(self, i, z):
        self.array[i] = self.d2udb(z)
