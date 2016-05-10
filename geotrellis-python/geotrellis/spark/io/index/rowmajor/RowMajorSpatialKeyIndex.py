from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.io.index.KeyIndex import KeyIndex

class RowMajorSpatialKeyIndex(KeyIndex[SpatialKey]):
    def __init__(self, keyBounds):
        self._keyBounds = keyBounds
        self.minCol = keyBounds.minKey.col
        self.minRow = keyBounds.minKey.row
        self.layoutCols = keyBounds.maxKey.col - keyBounds.minKey.col + 1

    def toIndex(self, col, row = None):
        if isinstance(col, SpatialKey) and row is None:
            key = col
            col, row = key.col, key.row
        return self.layoutCols * (row - self.minRow) + (col - self.minCol)

    def indexRanges(keyRange):
        minKey = keyRange[0]
        maxKey = keyRange[1]

        colMin = minKey.col
        rowMin = minKey.row
        colMax = maxKey.col
        rowMax = maxKey.row

        cols = colMax - colMin + 1
        rows = rowMax - rowMin

        result = [None] * (rows + 1)

        for i in xrange(0, rows):
            row = rowMin + i
            _min = self.toIndex(colMin, row)
            result[i] = (_min, _min + cols - 1)

        return result
