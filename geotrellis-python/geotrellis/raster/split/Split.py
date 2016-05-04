from geotrellis.raster.GridBounds import GridBounds
from geotrellis.raster.CroppedTile import CroppedTile

class _Split(object):
    @property
    def Options(self):
        return _Options

    def __call__(self, tile, tileLayout, options = _Options.DEFAULT):
        tileCols = tileLayout.tileCols
        tileRows = tileLayout.tileRows

        tiles = [None] * (tileLayout.layoutCols * tileLayout.layoutRows)
        for layoutRow in xrange(0, tileLayout.layoutRows):
            for layoutCol in xrange(0, tileLayout.layoutCols):
                firstCol = layoutCol * tileCols
                def getLastCol():
                    x = firstCol + tileCols - 1
                    if (not options.extend) and x > tile.cols - 1:
                        return tile.cols - 1
                    else:
                        return x
                lastCol = getLastCol()
                firstRow = layoutRow * tileRows
                def getLastRow():
                    x = firstRow + tileRows - 1
                    if (not options.extend) and x > tile.rows - 1:
                        return tile.rows - 1
                    else:
                        return x
                lastRow = getLastRow()
                gb = GridBounds(firstCol, firstRow, lastCol, lastRow)
                ct = CroppedTile(tile, gb)
                tiles[layoutRow * tileLayout.layoutCols + layoutCol] = ct if options.cropped else ct.toArrayTile()
        return tiles

Split = _Split

class _Options(object):

    DEFAULT = _Options()

    def __init__(self, cropped = True, extend = True):
        self.cropped = cropped
        self.extend = extend

    def __eq__(self, other):
        if not isinstance(other, _Options):
            return False
        return self.cropped == other.cropped and self.extend == other.extend

    def __hash__(self):
        return hash((self.cropped, self.extend))
