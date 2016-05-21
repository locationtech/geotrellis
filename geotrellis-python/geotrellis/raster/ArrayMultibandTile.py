from __future__ import absolute_import
from geotrellis.raster.MultibandTile import MultibandTile
from geotrellis.raster.ArrayTile import ArrayTile

class ArrayMultibandTile(MultibandTile):
    implicits = dict(MultibandTile.implicits)

    def __init__(self, *bands):
        if len(bands) == 1 and isinstance(bands[0], list):
            bands = bands[0]
        self._bands = bands
        self.bandCount = len(bands)
        if self.bandCount == 0:
            raise Exception("Band count must be greater than 0")

        cellType = bands[0].cellType
        cols = bands[0].cols
        rows = bands[0].rows

        self._cellType = cellType
        self._cols = cols
        self._rows = rows

        for i in xrange(1, self.bandCount):
            if not bands[i].cellType == cellType:
                raise Exception(
                        "Band {i} cell type does not match, " +
                        "{actual} != {exp}".format(
                            i=i, actual=bands[i].cellType, exp=cellType))
            if bands[i].cols != cols:
                raise Exception(
                        "Band {i} cols does not match, " +
                        "{actual} != {exp}".format(
                            i=i, actual=bands[i].cols, exp=cols))
            if bands[i].rows != rows:
                raise Exception(
                        "Band {i} rows does not match, " +
                        "{actual} != {exp}".format(
                            i=i, actual=bands[i].rows, exp=rows))

    @property
    def cellType(self):
        return self._cellType

    @property
    def cols(self):
        return self._cols

    @property
    def rows(self):
        return self._rows

    def band(self, bandIndex):
        if bandIndex >= self.bandCount:
            raise Exception("Band {ind} does not exist".format(ind = bandIndex))
        return self._bands[bandIndex]

    @property
    def bands(self):
        return self._bands[:]

    def _validateBand(self, i):
        if i >= self.bandCount:
            raise Exception(
                    "Band index out of bounds. Band Count: {count} " +
                    "Requested Band Index: {i}".format(
                        count = self.bandCount, i = i))

    def convert(self, newCellType):
        newBands = []
        for i in xrange(0, self.bandCount):
            band = self.band(i).convert(newCellType)
            newBands.append(band)
        return ArrayMultibandTile(newBands)

    def _map(self, first, second = None):
        if second is None:
            f = first
            newBands = [None] * self.bandCount
            for b in xrange(0, self.bandCount):
                newBands[b] = self.band(b).map(lambda z: f(b, z))

            return ArrayMultibandTile(newBands)
        elif isinstance(first, int):
            b0, f = first, second

            self._validateBand(b0)
            newBands = self._bands[:]
            newBands[b0] = self.band(b0).map(f)

            return ArrayMultibandTile(newBands)
        else:
            subset, f = first, second

            newBands = [None] * self.bandCount
            subset = set(subset)

            for b in subset:
                if b < 0 or b >= self.bandCount:
                    raise Exception("All elements of subset must be present")

            for b in xrange(0, self.bandCount):
                if b in subset:
                    newBands[b] = self.band(b).map(lambda z: f(b, z))
                elif self.cellType.isFloatingPoint:
                    newBands[b] = self.band(b).map(lambda z: z)
                else:
                    newBands[b] = self.band(b)

            return ArrayMultibandTile(newBands)

    def mapDouble(self, first, second = None):
        if second is None:
            f = first
            newBands = [None] * self.bandCount
            for b in xrange(0, self.bandCount):
                newBands[b] = self.band(b).mapDouble(lambda z: f(b, z))

            return ArrayMultibandTile(newBands)
        elif isinstance(first, int):
            b0, f = first, second

            self._validateBand(b0)
            newBands = self._bands[:]
            newBands[b0] = self.band(b0).mapDouble(f)

            return ArrayMultibandTile(newBands)
        else:
            subset, f = first, second

            newBands = [None] * self.bandCount
            subset = set(subset)

            for b in subset:
                if b < 0 or b >= self.bandCount:
                    raise Exception("All elements of subset must be present")

            for b in xrange(0, self.bandCount):
                if b in subset:
                    newBands[b] = self.band(b).mapDouble(lambda z: f(b, z))
                elif self.cellType.isFloatingPoint:
                    newBands[b] = self.band(b)
                else:
                    newBands[b] = self.band(b).mapDouble(lambda z: z)

            return ArrayMultibandTile(newBands)

    def _foreach(self, first, second = None):
        if second is None:
            f = first
            for i in xrange(0, self.bandCount):
                self.band(i).foreach(lambda z: f(i, z))
        else:
            b0, f = first, second

            self._validateBand(b0)
            self.band(b0).foreach(f)

    def _foreachDouble(self, first, second = None):
        if second is None:
            f = first
            for i in xrange(0, self.bandCount):
                self.band(i).foreachDouble(lambda z: f(i, z))
        else:
            b0, f = first, second

            self._validateBand(b0)
            self.band(b0).foreachDouble(f)

    def combine(self, first, second = None, third = None):
        if second is None and third is None:
            f = first

            result = ArrayTile.empty(self.cellType, self.cols, self.rows)
            arr = [None] * self.bandCount
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    for b in xrange(0, self.bandCount):
                        arr[b] = self.band(b).get(col, row)
                    result.set(col, row, f(arr))
            return result

        elif third is None:
            subset, f = first, second

            for b in subset:
                if b < 0 or b >= self.bandCount:
                    raise Exception("All elements of subset must be present")

            result = ArrayTile.empty(self.cellType, self.cols, self.rows)
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    data = map(lambda b: self.band(b).get(col, row), subset)
                    result.set(col, row, f(data))

            return result
        else:
            b0, b1, f = first, second, third

            band1 = self.band(b0)
            band2 = self.band(b1)
            result = ArrayTile.empty(self.cellType, self.cols, self.rows)
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    result.set(col, row,
                            f(band1.get(col, row),
                                band2.get(col, row)))
            return result

    def combineDouble(self, first, second = None, third = None):
        if second is None and third is None:
            f = first

            result = ArrayTile.empty(self.cellType, self.cols, self.rows)
            arr = [None] * self.bandCount
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    for b in xrange(0, self.bandCount):
                        arr[b] = self.band(b).getDouble(col, row)
                    result.setDouble(col, row, f(arr))
            return result

        elif third is None:
            subset, f = first, second

            for b in subset:
                if b < 0 or b >= self.bandCount:
                    raise Exception("All elements of subset must be present")

            result = ArrayTile.empty(self.cellType, self.cols, self.rows)
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    data = map(lambda b: self.band(b).getDouble(col, row), subset)
                    result.setDouble(col, row, f(data))

            return result
        else:
            b0, b1, f = first, second, third

            band1 = self.band(b0)
            band2 = self.band(b1)
            result = ArrayTile.empty(self.cellType, self.cols, self.rows)
            for row in xrange(0, self.rows):
                for col in xrange(0, self.cols):
                    result.setDouble(col, row,
                            f(band1.getDouble(col, row),
                                band2.getDouble(col, row)))
            return result

    def subsetBands(self, bandSequence):
        newBands = []

        if len(bandSequence) > self.bandCount:
            raise Exception(
                    "The length of a bandSequence argument is " +
                    "greater than self band count.")
        for j in bandSequence:
            newBands.append(self.band(j))

        return ArrayMultibandTile(newBands)

    def __eq__(self, other):
        if (other is None or
                (not isinstance(other, ArrayMultibandTile)) or
                self.bandCount != other.bandCount):
            return False
        for i in xrange(0, self.bandCount):
            if not self.band(i) == other.band(i):
                return False
        return True

    @staticmethod
    def alloc(t, bands, cols, rows):
        return ArrayMultibandTile([ArrayTile.alloc(t, cols, rows) for i in xrange(0, bands)])

    @staticmethod
    def empty(t, bands, cols, rows):
        return ArrayMultibandTile([ArrayTile.empty(t, cols, rows) for i in xrange(0, bands)])
