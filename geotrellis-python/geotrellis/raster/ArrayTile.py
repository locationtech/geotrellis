from geotrellis.raster.Tile import Tile
from geotrellis.raster.ConstantTile import ConstantTile
from geotrellis.raster.CompositeTile import CompositeTile
from geotrellis.raster.CroppedTile import CroppedTile
from geotrellis.raster.BitArrayTile import BitArrayTile
from geotrellis.raster.ByteArrayTile import ByteArrayTile, ByteConstantNoDataArrayTile, ByteRawArrayTile
from geotrellis.raster.UByteArrayTile import UByteArrayTile
from geotrellis.raster.ShortArrayTile import ShortArrayTile, ShortConstantNoDataArrayTile, ShortRawArrayTile
from geotrellis.raster.UShortArrayTile import UShortArrayTile
from geotrellis.raster.IntArrayTile import IntArrayTile, IntConstantNoDataArrayTile, IntRawArrayTile
from geotrellis.raster.FloatArrayTile import FloatArrayTile, FloatConstantNoDataArrayTile, FloatRawArrayTile
from geotrellis.raster.DoubleArrayTile import DoubleArrayTile, DoubleConstantNoDataArrayTile, DoubleRawArrayTile

from geotrellis.raster.package_scala import assertEqualDimensions

import array

class ArrayTile(Tile):
    def toArrayTile(self):
        return self

    def convert(self, targetCellType):
        tile = ArrayTile.alloc(targetCellType, self.cols, self.rows)
        if not self.cellType.isFloatingPoint:
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    tile.set(col, row, self.get(col, row))
        else:
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    tile.setDouble(col, row, self.getDouble(col, row))
        return tile

    def foreach(self, f):
        length = self.size
        i = 0
        while i < length:
            f(self[i])
            i += 1

    def foreachDouble(self, f):
        length = self.size
        i = 0
        while i < length:
            f(self.applyDouble(i))
            i += 1

    def foreachIntVisitor(visitor):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                visitor(col, row, self.get(col, row))

    def foreachDoubleVisitor(visitor):
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                visitor(col, row, self.getDouble(col, row))

    def map(self, f):
        output = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        i = 0
        length = self.size
        while i < length:
            output[i] = f(self[i])
            i += 1
        return output

    def mapDouble(self, f):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        i = 0
        length = self.size
        while i < length:
            tile.updateDouble(i, f(self.applyDouble(i)))
            i += 1
        return tile

    def mapIntMapper(self, mapper):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.set(col, row, mapper(col, row, self.get(col, row)))
        return tile

    def mapDoubleMapper(self, mapper):
        tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
        for row in xrange(0, self.rows):
            for col in xrange(0, self.cols):
                tile.setDouble(col, row, mapper(col, row, self.getDouble(col, row)))
        return tile

    def combine(self, other, f):
        if isinstance(other, ConstantTile):
            return other.combine(self, f)
        elif isinstance(other, CompositeTile):
            return other.combine(self, lambda z1, z2: f(z2, z1))
        elif isinstance(other, CroppedTile):
            return other.combine(self, lambda z1, z2: f(z2, z1))
        elif isinstance(other, ArrayTile):
            assertEqualDimensions(self, other)
            output = ArrayTile.alloc(self.cellType.union(other.cellType), self.cols, self.rows)
            i = 0
            length = self.size
            while i < length:
                output[i] = f(self[i], other[i])
                i += 1
            return output

    def combineDouble(self, other, f):
        if isinstance(other, ConstantTile):
            return other.combineDouble(self, f)
        elif isinstance(other, CompositeTile):
            return other.combineDouble(self, lambda z1, z2: f(z2, z1))
        #elif isinstance(other, CroppedTile):
        #    return other.combine(self, lambda z1, z2: f(z2, z1))
        elif isinstance(other, ArrayTile):
            assertEqualDimensions(self, other)
            output = ArrayTile.alloc(self.cellType.union(other.cellType), self.cols, self.rows)
            i = 0
            length = self.size
            while i < length:
                output.updateDouble(i, f(self.applyDouble(i), other.applyDouble(i)))
                i += 1
            return output

    def __eq__(self, other):
        if not isinstance(other, ArrayTile):
            return False
        if not other:
            return False
        length = self.size
        if length != other.size:
            return False
        i = 0
        while i < length:
            if self[i] != other[i]:
                return False
            i += 1
        return True

    def __getitem__(self, i):
        return self.apply(i)

    def apply(self, i):
        pass

    def applyDouble(self, i):
        pass

    def get(self, col, row):
        return self[row * self.cols + col]

    def getDouble(self, col, row):
        return self.applyDouble(row * self.cols + col)

    def copy(self):
        pass

    def toList(self):
        return self.toArray().tolist()

    def toListDouble(self):
        return self.toArrayDouble().tolist()

    def toArray(self):
        length = self.size
        arr = [None] * length
        i = 0
        while i < length:
            arr[i] = self[i]
            i += 1
        return array.array('i', arr)

    def toArrayDouble(self):
        length = self.size
        arr = [None] * length
        i = 0
        while i < length:
            arr[i] = self.applyDouble(i)
            i += 1
        return array.array('d', arr)

    def toBytes(self):
        pass

    @staticmethod
    def alloc(t, cols, rows):
        if isinstance(t, BitCells):
            return BitArrayTile.ofDim(cols, rows)
        elif isinstance(t, ByteCells):
            return ByteArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, UByteCells):
            return UByteArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, ShortCells):
            return ShortArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, UShortCells):
            return UShortArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, IntCells):
            return IntArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, FloatCells):
            return FloatArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, DoubleCells):
            return DoubleArrayTile.ofDim(cols, rows, t)

    @staticmethod
    def empty(t, cols, rows):
        if isinstance(t, BitCells):
            return BitArrayTile.empty(cols, rows)
        elif isinstance(t, ByteCells):
            return ByteArrayTile.empty(cols, rows, t)
        elif isinstance(t, UByteCells):
            return UByteArrayTile.empty(cols, rows, t)
        elif isinstance(t, ShortCells):
            return ShortArrayTile.empty(cols, rows, t)
        elif isinstance(t, UShortCells):
            return UShortArrayTile.empty(cols, rows, t)
        elif isinstance(t, IntCells):
            return IntArrayTile.empty(cols, rows, t)
        elif isinstance(t, FloatCells):
            return FloatArrayTile.empty(cols, rows, t)
        elif isinstance(t, DoubleCells):
            return DoubleArrayTile.empty(cols, rows, t)
    
    @staticmethod
    def fromBytes(bytesarray, t, cols, rows):
        if isinstance(t, BitCells):
            return BitArrayTile.fromBytes(bytesarray, cols, rows)
        elif isinstance(t, ByteCells):
            return ByteArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, UByteCells):
            return UByteArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, ShortCells):
            return ShortArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, UShortCells):
            return UShortArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, IntCells):
            return IntArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, FloatCells):
            return FloatArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, DoubleCells):
            return DoubleArrayTile.fromBytes(bytesarray, cols, rows, t)

    @staticmethod
    def applyStatic(arr, cols, rows):
        code = arr.typecode
        if code == 'b':
            return ByteConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'h':
            return ShortConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'i':
            return IntConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'f':
            return FloatConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'd':
            return DoubleConstantNoDataArrayTile(arr, cols, rows)

class _RawArrayTile(object):
    def __call__(self, arr, cols, rows)
        code = arr.typecode
        if code == 'b':
            return ByteRawArrayTile(arr, cols, rows)
        elif code == 'h':
            return ShortRawArrayTile(arr, cols, rows)
        elif code == 'i':
            return IntRawArrayTile(arr, cols, rows)
        elif code == 'f':
            return FloatRawArrayTile(arr, cols, rows)
        elif code == 'd':
            return DoubleRawArrayTile(arr, cols, rows)

RawArrayTile = _RawArrayTile()
