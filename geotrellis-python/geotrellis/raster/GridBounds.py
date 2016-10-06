from __future__ import absolute_import
from geotrellis.python.util.utils import fold_left, flat_map
from geotrellis.raster.CellGrid import CellGrid

class GridBounds(object):
    def __init__(self, colmin, rowmin = None, colmax = None, rowmax = None):
        if isinstance(colmin, CellGrid):
            cellgrid = colmin
            colmin = 0
            rowmin = 0
            colmax = cellgrid.cols-1
            rowmax = cellgrid.rows-1
        self.colMin = colmin
        self.rowMin = rowmin
        self.colMax = colmax
        self.rowMax = rowmax

    def __eq__(self, other):
        if not isinstance(other, GridBounds):
            return False
        return (self.colMin == other.colMin and
                self.rowMin == other.rowMin and
                self.colMax == other.colMax and
                self.rowMax == other.rowMax)

    def __hash__(self):
        return hash((self.colMin, self.rowMin, self.colMax, self.rowMax))

    @property
    def width(self):
        return self.colMax - self.colMin + 1

    @property
    def height(self):
        return self.rowMax - self.rowMin + 1

    @property
    def size(self):
        return self.width * self.height

    @property
    def isEmpty(self):
        return self.size() == 0

    def contains(self, col, row):
        return ((self.colMin <= col and col <= self.colMax) and 
                (self.rowMin <= row and row <= self.rowMax))

    def intersects(self, other):
        return (not (self.colMax < other.colMin or other.colMax < self.colMin) and
                not (self.rowMax < other.rowMin or other.rowMax < self.rowMin))

    def __sub__(self, other):
        return self.minus(other)

    def minus(self, other):
        if not self.intersects(other):
            return [self]

        overlap_colMin = max(self.colMin, other.colMin)
        overlap_colMax = min(self.colMax, other.colMax)
        overlap_rowMin = max(self.rowMin, other.rowMin)
        overlap_rowMax = min(self.rowMax, other.rowMax)

        result = []

        if self.colMin < overlap_colMin:
            result.append(GridBounds(self.colMin, self.rowMin, overlap_colMin-1, self.rowMax))

        if overlap_colMax < self.colMax:
            result.append(GridBounds(overlap_colMax+1, self.rowMin, self.colMax, self.rowMax))

        if self.rowMin < overlap_rowMin:
            result.append(GridBounds(overlap_colMin, self.rowMin, overlap_colMax, overlap_rowMin-1))

        if overlap_rowMax < self.rowMax:
            result.append(GridBounds(overlap_colMin, overlap_rowMax+1, overlap_colMax, self.rowMax))

        return result

    @property
    def coords(self):
        arr = [None] * self.size()
        for row in xrange(0, self.height()):
            for col in xrange(0, self.width()):
                index = row*self.width() + col
                arr[index] = (col + self.colMin, row + self.rowMin)
        return arr

    def intersection(self, other):
        if isinstance(other, CellGrid):
            other = GridBounds(other)
        if not self.intersects(other):
            return None
        return GridBounds(
                max(self.colMin, other.colMin),
                max(self.rowMin, other.rowMin),
                min(self.colMax, other.colMax),
                min(self.rowMax, other.rowMax))

    @staticmethod
    def envelope(keys):
        colMin = sys.maxint
        colMax = -(sys.maxint + 1)
        rowMin = sys.maxint
        rowMax = -(sys.maxint + 1)

        for key in keys:
            col = key[0]
            row = key[1]
            if col < colMin:
                colMin = col
            if col > colMax:
                colMax = col
            if row < rowMin:
                rowMin = row
            if row > rowMax:
                rowMax = row

        return GridBounds(colMin, rowMin, colMax, rowMax)

    @staticmethod
    def distinct(grid_bounds_list):
        def func(acc, bounds):
            def inner_func(cuts, bounds):
                return flat_map(cuts, lambda a: a - bounds)
            return acc + fold_left(acc, [bounds], inner_func)
        return fold_left(grid_bounds_list, [], func)
