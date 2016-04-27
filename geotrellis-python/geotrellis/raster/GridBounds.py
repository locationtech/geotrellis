from geotrellis.python.util.utils import fold_left, flat_map

class GridBounds(object):
    def __init__(self, colmin, rowmin, colmax, rowmax):
        self.col_min = colmin
        self.row_min = rowmin
        self.col_max = colmax
        self.row_max = rowmax

    def width(self):
        return self.col_max - self.col_min + 1

    def height(self):
        return self.row_max - self.row_min + 1

    def size(self):
        return self.width() * self.height()

    def is_empty(self):
        return self.size() == 0

    def contains(self, col, row):
        return ((self.col_min <= col and col <= self.col_max) and 
                (self.row_min <= row and row <= self.row_max))

    def intersects(self, other):
        return (not (self.col_max < other.col_min or other.col_max < self.col_min) and
                not (self.row_max < other.row_min or other.row_max < self.row_min))

    def __sub__(self, other):
        return self.minus(other)

    def minus(self, other):
        if not self.intersects(other):
            return [self]

        overlap_col_min = max(self.col_min, other.col_min)
        overlap_col_max = min(self.col_max, other.col_max)
        overlap_row_min = max(self.row_min, other.row_min)
        overlap_row_max = min(self.row_max, other.row_max)

        result = []

        if self.col_min < overlap_col_min:
            result.append(GridBounds(self.col_min, self.row_min, overlap_col_min-1, self.row_max))

        if overlap_col_max < self.col_max:
            result.append(GridBounds(overlap_col_max+1, self.row_min, self.col_max, self.row_max))

        if self.row_min < overlap_row_min:
            result.append(GridBounds(overlap_col_min, self.row_min, overlap_col_max, overlap_row_min-1))

        if overlap_row_max < self.row_max:
            result.append(GridBounds(overlap_col_min, overlap_row_max+1, overlap_col_max, self.row_max))

        return result

    def coords(self):
        arr = [None] * self.size()
        for row in xrange(0, self.height()):
            for col in xrange(0, self.width()):
                index = row*self.width() + col
                arr[index] = (col + self.col_min, row + self.row_min)
        return arr

    def intersection(self, other):
        if isinstance(other, CellGrid):
            other = grid_bounds(other)
        if not self.intersects(other):
            return None
        return GridBounds(
                max(self.col_min, other.col_min),
                max(self.row_min, other.row_min),
                min(self.col_max, other.col_max),
                min(self.row_max, other.row_max))

    @staticmethod
    def envelope(keys):
        col_min = sys.maxint
        col_max = -(sys.maxint + 1)
        row_min = sys.maxint
        row_max = -(sys.maxint + 1)

        for key in keys:
            col = key[0]
            row = key[1]
            if col < col_min:
                col_min = col
            if col > col_max:
                col_max = col
            if row < row_min:
                row_min = row
            if row > row_max:
                row_max = row

        return GridBounds(col_min, row_min, col_max, row_max)

    @staticmethod
    def distinct(grid_bounds_list):
        def func(acc, bounds):
            def inner_func(cuts, bounds):
                return flat_map(cuts, lambda a: a - bounds)
            acc + fold_left(acc, [bounds], inner_func)
        return fold_left(grid_bounds_list, [], func)

def grid_bounds(cellgrid):
    return GridBounds(0, 0, cellgrid.cols-1, cellgrid.rows-1)

