from __future__ import absolute_import
from geotrellis.raster.ArrayTile import ArrayTile

class MutableArrayTile(ArrayTile):
    def mutable(self):
        return self

    def __setitem__(self, i, z):
        self._update(i, z)

    def _update(self, i, z):
        pass

    def update(self, first, second, third = None):
        if third is None:
            return self._update(first, second)
        colOffset = first
        rowOffset = second
        update = third

        if self.cellType.isFloatingPoint:
            for r in xrange(0, update.rows):
                for c in xrange(0, update.cols):
                    self.setDouble(c + colOffset, r + rowOffset, update.getDouble(c, r))
        else:
            for r in xrange(0, update.rows):
                for c in xrange(0, update.cols):
                    self.set(c + colOffset, r + rowOffset, update.get(c, r))

    def updateDouble(self, i, z):
        pass

    def set(self, col, row, value):
        self.update(row * self.cols + col, value)

    def setDouble(self, col, row, value):
        self.update(row * self.cols + col, value)
