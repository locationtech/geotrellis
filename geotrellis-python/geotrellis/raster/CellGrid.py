# from geotrellis.raster.GridBounds import GridBounds

class CellGrid(object):
    @property
    def cols(self):
        pass
    @property
    def rows(self):
        pass
    @property
    def cellType(self):
        pass
    @property
    def dimensions(self):
        return (self.cols, self.rows)
    @property
    def gridBounds(self):
        from geotrellis.raster.GridBounds import GridBounds
        return GridBounds(0, 0, self.cols-1, self.rows-1)
    @property
    def size(self):
        return self.cols * self.rows

