from __future__ import absolute_import
from geotrellis.raster.FloatArrayTile import FloatArrayTile

class TestFileSpatialTiles(object):
    def __init__(self, tileLayout):
        self._tileLayout = tileLayout

    def __call__(self, key):
        tile = FloatArrayTile.empty(self._tileLayout.tileCols, self._tileLayout.tileRows)

        for row in xrange(0, self._tileLayout.tileRows):
            for col in xrange(0, self._tileLayout.tileCols):
                tile.setDouble(col, row, self.value(key, col, row))

        return tile

    def value(self, key, col, row):
        pass

class ConstantSpatialTiles(TestFileSpatialTiles):
    def __init__(self, tileLayout, f):
        TestFileSpatialTiles.__init__(self, tileLayout)
        self._f = f

    def value(self, key, col, row):
        return self._f

class IncreasingSpatialTiles(TestFileSpatialTiles):
    def __init__(self, tileLayout, gridBounds):
        TestFileSpatialTiles.__init__(self, tileLayout)
        self._gridBounds = gridBounds

    def value(self, key, col, row):
        tileCol = key.col
        tileRow = key.row
        tc = tileCol - self._gridBounds.colMin
        tr = tileRow - self._gridBounds.rowMin
        r = (tr * self._tileLayout.tileRows + row) * (self._tileLayout.tileCols * self._gridBounds.width)
        c = tc * self._tileLayout.tileCols + col
        return r + c

class DecreasingSpatialTiles(TestFileSpatialTiles):
    def __init__(self, tileLayout, gridBounds):
        TestFileSpatialTiles.__init__(self, tileLayout)
        self._gridBounds = gridBounds

    def value(self, key, col, row):
        tileCol = key.col
        tileRow = key.row
        gridBounds = self._gridBounds
        tileLayout = self._tileLayout
        tc = tileCol - gridBounds.colMin
        tr = tileRow - gridBounds.rowMin
        r = ( ((gridBounds.height * tileLayout.tileRows) -
                    (tr * tileLayout.tileRows + row) - 1) *
                (tileLayout.tileCols * gridBounds.width))
        c = ((tileLayout.tileCols * gridBounds.width - 1) -
                (tc * tileLayout.tileCols + col))
        return r + c

class EveryOtherSpatialTiles(IncreasingSpatialTiles):
    def __init__(self, tileLayout, gridBounds, firstValue, secondValue):
        IncreasingSpatialTiles.__init__(self, tileLayout, gridBounds)
        self._firstValue = firstValue
        self._secondValue = secondValue

    def value(self, key, col, row):
        if IncreasingSpatialTiles.value(self, key, col, row) % 2 == 0:
            return self._firstValue
        else:
            return self._secondValue

class ModSpatialTiles(IncreasingSpatialTiles):
    def __init__(self, tileLayout, gridBounds, mod):
        IncreasingSpatialTiles.__init__(self, tileLayout, gridBounds)
        self._mod = mod

    def value(self, key, col, row):
        superVal = IncreasingSpatialTiles.value(self, key, col, row)
        return superVal % self._mod
