from __future__ import absolute_import
from geotrellis.raster.Tile import Tile
#from geotrellis.raster.ConstantTile import ConstantTile

#from geotrellis.raster.CompositeTile import CompositeTile
#from geotrellis.raster.CroppedTile import CroppedTile
#from geotrellis.raster.BitArrayTile import BitArrayTile

#from geotrellis.raster.ByteArrayTile import ByteArrayTile
#from geotrellis.raster.ByteArrayTile import ByteConstantNoDataArrayTile
#from geotrellis.raster.ByteArrayTile import ByteRawArrayTile

#from geotrellis.raster.UByteArrayTile import UByteArrayTile

#from geotrellis.raster.ShortArrayTile import ShortArrayTile
#from geotrellis.raster.ShortArrayTile import ShortConstantNoDataArrayTile
#from geotrellis.raster.ShortArrayTile import ShortRawArrayTile

#from geotrellis.raster.UShortArrayTile import UShortArrayTile

#from geotrellis.raster.IntArrayTile import IntArrayTile
#from geotrellis.raster.IntArrayTile import IntConstantNoDataArrayTile
#from geotrellis.raster.IntArrayTile import IntRawArrayTile

#from geotrellis.raster.FloatArrayTile import FloatArrayTile
#from geotrellis.raster.FloatArrayTile import FloatConstantNoDataArrayTile
#from geotrellis.raster.FloatArrayTile import FloatRawArrayTile

#from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
#from geotrellis.raster.DoubleArrayTile import DoubleConstantNoDataArrayTile
#from geotrellis.raster.DoubleArrayTile import DoubleRawArrayTile

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
        from geotrellis.raster.ConstantTile import ConstantTile
        from geotrellis.raster.CompositeTile import CompositeTile
        from geotrellis.raster.CroppedTile import CroppedTile
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
        from geotrellis.raster.ConstantTile import ConstantTile
        from geotrellis.raster.CompositeTile import CompositeTile
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
            from geotrellis.raster.BitArrayTile import BitArrayTile
            return BitArrayTile.ofDim(cols, rows)
        elif isinstance(t, ByteCells):
            from geotrellis.raster.ByteArrayTile import ByteArrayTile
            return ByteArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, UByteCells):
            from geotrellis.raster.UByteArrayTile import UByteArrayTile
            return UByteArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, ShortCells):
            from geotrellis.raster.ShortArrayTile import ShortArrayTile
            return ShortArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, UShortCells):
            from geotrellis.raster.UShortArrayTile import UShortArrayTile
            return UShortArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, IntCells):
            from geotrellis.raster.IntArrayTile import IntArrayTile
            return IntArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, FloatCells):
            from geotrellis.raster.FloatArrayTile import FloatArrayTile
            return FloatArrayTile.ofDim(cols, rows, t)
        elif isinstance(t, DoubleCells):
            from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
            return DoubleArrayTile.ofDim(cols, rows, t)

    @staticmethod
    def empty(t, cols, rows):
        if isinstance(t, BitCells):
            from geotrellis.raster.BitArrayTile import BitArrayTile
            return BitArrayTile.empty(cols, rows)
        elif isinstance(t, ByteCells):
            from geotrellis.raster.ByteArrayTile import ByteArrayTile
            return ByteArrayTile.empty(cols, rows, t)
        elif isinstance(t, UByteCells):
            from geotrellis.raster.UByteArrayTile import UByteArrayTile
            return UByteArrayTile.empty(cols, rows, t)
        elif isinstance(t, ShortCells):
            from geotrellis.raster.ShortArrayTile import ShortArrayTile
            return ShortArrayTile.empty(cols, rows, t)
        elif isinstance(t, UShortCells):
            from geotrellis.raster.UShortArrayTile import UShortArrayTile
            return UShortArrayTile.empty(cols, rows, t)
        elif isinstance(t, IntCells):
            from geotrellis.raster.IntArrayTile import IntArrayTile
            return IntArrayTile.empty(cols, rows, t)
        elif isinstance(t, FloatCells):
            from geotrellis.raster.FloatArrayTile import FloatArrayTile
            return FloatArrayTile.empty(cols, rows, t)
        elif isinstance(t, DoubleCells):
            from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
            return DoubleArrayTile.empty(cols, rows, t)
    
    @staticmethod
    def fromBytes(bytesarray, t, cols, rows):
        if isinstance(t, BitCells):
            from geotrellis.raster.BitArrayTile import BitArrayTile
            return BitArrayTile.fromBytes(bytesarray, cols, rows)
        elif isinstance(t, ByteCells):
            from geotrellis.raster.ByteArrayTile import ByteArrayTile
            return ByteArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, UByteCells):
            from geotrellis.raster.UByteArrayTile import UByteArrayTile
            return UByteArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, ShortCells):
            from geotrellis.raster.ShortArrayTile import ShortArrayTile
            return ShortArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, UShortCells):
            from geotrellis.raster.UShortArrayTile import UShortArrayTile
            return UShortArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, IntCells):
            from geotrellis.raster.IntArrayTile import IntArrayTile
            return IntArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, FloatCells):
            from geotrellis.raster.FloatArrayTile import FloatArrayTile
            return FloatArrayTile.fromBytes(bytesarray, cols, rows, t)
        elif isinstance(t, DoubleCells):
            from geotrellis.raster.DoubleArrayTile import DoubleArrayTile
            return DoubleArrayTile.fromBytes(bytesarray, cols, rows, t)

    @staticmethod
    def applyStatic(arr, cols, rows):
        code = arr.typecode
        if code == 'b':
            from geotrellis.raster.ByteArrayTile import ByteConstantNoDataArrayTile
            return ByteConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'h':
            from geotrellis.raster.ShortArrayTile import ShortConstantNoDataArrayTile
            return ShortConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'i':
            from geotrellis.raster.IntArrayTile import IntConstantNoDataArrayTile
            return IntConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'f':
            from geotrellis.raster.FloatArrayTile import FloatConstantNoDataArrayTile
            return FloatConstantNoDataArrayTile(arr, cols, rows)
        elif code == 'd':
            from geotrellis.raster.DoubleArrayTile import DoubleConstantNoDataArrayTile
            return DoubleConstantNoDataArrayTile(arr, cols, rows)

class _RawArrayTile(object):
    def __call__(self, arr, cols, rows):
        code = arr.typecode
        if code == 'b':
            from geotrellis.raster.ByteArrayTile import ByteRawArrayTile
            return ByteRawArrayTile(arr, cols, rows)
        elif code == 'h':
            from geotrellis.raster.ShortArrayTile import ShortRawArrayTile
            return ShortRawArrayTile(arr, cols, rows)
        elif code == 'i':
            from geotrellis.raster.IntArrayTile import IntRawArrayTile
            return IntRawArrayTile(arr, cols, rows)
        elif code == 'f':
            from geotrellis.raster.FloatArrayTile import FloatRawArrayTile
            return FloatRawArrayTile(arr, cols, rows)
        elif code == 'd':
            from geotrellis.raster.DoubleArrayTile import DoubleRawArrayTile
            return DoubleRawArrayTile(arr, cols, rows)

RawArrayTile = _RawArrayTile()
