from __future__ import absolute_import
from geotrellis.raster.CellGrid import CellGrid
from geotrellis.raster.CellSet import CellSet
#from geotrellis.raster.ArrayTile import ArrayTile
from geotrellis.raster.package_scala import isData, isNoData, NODATA

import math
import inspect

class Tile(CellGrid):

    def _map(self, f):
        argspec = inspect.getargspec(f)
        paramsCount = len(argspec[0])
        if paramsCount == 1:
            return self._map(f)
        else:
            return self.mapIntMapper(f)

    def mapDouble(self, f):
        argspec = inspect.getargspec(f)
        paramsCount = len(argspec[0])
        if paramsCount == 1:
            return self._mapDouble(f)
        else:
            return self.mapDoubleMapper(f)
        
    def _foreach(self, f):
        argspec = inspect.getargspec(f)
        paramsCount = len(argspec[0])
        if paramsCount == 1:
            return self._foreach(f)
        else:
            return self.foreachIntVisitor(f)

    def foreachDouble(self, f):
        argspec = inspect.getargspec(f)
        paramsCount = len(argspec[0])
        if paramsCount == 1:
            return self._foreachDouble(f)
        else:
            return self.foreachDoubleVisitor(f)

    def dualForeach(self, f, g):
        if self.cellType.isFloatingPoint:
            self.foreachDouble(g)
        else:
            self.foreach(f)

    def mapIfSet(self, func):
        return self.map(lambda i: i if self.isNoData(i) else func(i))

    def mapIfSetDouble(self, func):
        return self.mapDouble(lambda i: i if self.isNoData(i) else func(i))

    def dualMap(self, f, g):
        if self.cellType.isFloatingPoint:
            return self.mapDouble(g)
        else:
            return self.map(f)

    def dualMapIfSet(self, f, g):
        if self.cellType.isFloatingPoint:
            return self.mapIfSetDouble(g)
        else:
            return self.mapIfSet(f)

    def dualCombine(self, r2, f, g):
        if self.cellType.union(r2.cellType).isFloatingPoint:
            return self.combineDouble(r2, g)
        else:
            return self.combine(r2, f)

    @property
    def isNoDataTile(self):
        c, r = 0, 0
        while r < self.rows:
            while c < self.cols:
                if isData(self.get(c, r)):
                    return False
                c += 1
            c = 0
            r += 1
        return True

    def normalize(self, oldMin, oldMax, newMin, newMax):
        dnew = newMax - newMin
        dold = oldMax - oldMin
        if dold <= 0 or dnew <= 0:
            raise Exception(
                    "Invalid parameters {omi}, {oma}, {nmi}, {nma}".format(
                        omi = oldMin, oma = oldMax, nmi = newMin, nma = newMax))
        func = lambda z: ( ((z - oldMin) * dnew) / dold ) + newMin
        if (isinstance(oldMin, float) and
                isinstance(oldMax, float) and
                isinstance(newMin, float) and
                isinstance(newMax, float)):
            return self.mapIfSetDouble(func)
        else:
            return self.mapIfSet(func)

    def rescale(self, newMin, newMax):
        if isinstance(newMin, float) and isinstance(newMax, float):
            mn, mx = self.findMinMaxDouble()
        else:
            mn, mx = self.findMinMax()
        return self.normalize(mn, mx, newMin, newMax)

    def downsample(self, newCols, newRows, func):
        colsPerBlock = int(math.ceil(self.cols / float(newCols)))
        rowsPerBlock = int(math.ceil(self.rows / float(newRows)))

        from geotrellis.raster.ArrayTile import ArrayTile
        tile = ArrayTile.empty(self.cellType, newCols, newRows)

        class DownsampleCellSet(CellSet):
            def focusOn(self, col, row):
                pass

        class TempDownsampleCellSet(DownsampleCellSet):
            focusCol = 0
            focusRow = 0

            def focusOn(self, col, row):
                self.focusCol = col
                self.focusRow = row

            def foreach(self, func):
                col = 0
                while col < colsPerBlock:
                    row = 0
                    while row < rowsPerBlock:
                        func(self.focusCol * colsPerBlock + col,
                                self.focusRow * rowsPerBlock + row)
                        row += 1
                    col += 1

        col = 0
        while col < newCols:
            row = 0
            while row < newRows:
                cellSet.focusOn(col, row)
                tile.set(col, row, func(cellSet))
                row += 1
            col += 1
        return tile

    def findMinMax(self):
        maxint = 2 ** 31 - 1
        zmin = maxint
        zmax = NODATA

        def func(z):
            if not isData(z):
                return
            zmin = min(zmin, z)
            zmax = max(zmax, z)

        self.foreach(func)
        if zmin == maxint:
            zmin = NODATA
        return (zmin, zmax)

    def findMinMaxDouble(self):
        zmin = float("nan")
        zmax = float("nan")

        def func(z):
            if not isData(z):
                return
            if isNoData(zmin):
                zmin = z
                zmax = z
            else:
                zmin = min(zmin, z)
                zmax = max(zmax, z)

        self.foreach(func)
        return (zmin, zmax)
