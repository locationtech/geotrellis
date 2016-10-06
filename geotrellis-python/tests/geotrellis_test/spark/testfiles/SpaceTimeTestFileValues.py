from __future__ import absolute_import
from geotrellis.raster.FloatArrayTile import FloatArrayTile

class TestFileSpaceTimeTiles(object):
    def __init__(self, tileLayout):
        self.tileLayout = tileLayout

    def __call__(self, key, timeIndex):
        tile = FloatArrayTile.empty(self.tileLayout.tileCols, self.tileLayout.tileRows)
        for row in xrange(0, self.tileLayout.tileRows):
            for col in xrange(0, self.tileLayout.tileCols):
                tile.setDouble(col, row, self.value(key, timeIndex, col, row))
        return tile

class ConstantSpaceTimeTestTiles(TestFileSpaceTimeTiles):
    def __init__(self, tileLayout, v):
        TestFileSpaceTimeTiles.__init__(self, tileLayout)
        self.v = v

    def value(self, key, timeIndex, col, row):
        return self.v

class CoordinateSpaceTimeTestTiles(TestFileSpaceTimeTiles):
    def __init__(self, tileLayout):
        TestFileSpaceTimeTiles.__init__(self, tileLayout)

    def value(self, key, timeIndex, col, row):
        layoutCol, layoutRow = key.col, key.row
        return (layoutCol * 1000.0) + layoutRow + (timeIndex / 1000.0)
