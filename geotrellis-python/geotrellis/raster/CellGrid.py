from . import *

class CellGrid(object):
    def cols(self):
        pass
    def rows(self):
        pass
    def cell_type(self):
        pass
    def dimensions(self):
        return (self.cols, self.rows)
    def grid_bounds(self):
        return GridBounds(0, 0, self.cols()-1, self.rows()-1)
    def size(self):
        return self.cols() * self.rows()

