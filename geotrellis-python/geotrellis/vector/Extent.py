from geotrellis.spark.io.package_scala import ExceptionWithCause
from geotrellis.vector.reproject.Implicits import reproject

import functools

class ExtentRangeError(ExceptionWithCause):
    def __init__(self, msg, **kwargs):
        ExceptionWithCause.__init__(self, msg, **kwargs)

class ProjectedExtent(object):
    def __init__(self, extent, crs):
        self.extent = extent
        self.crs = crs
    def __eq__(self, other):
        if not isinstance(other, ProjectedExtent):
            return False
        return self.extent == other.extent and self.crs == other.crs
    def __hash__(self):
        return hash((self.extent, self.crs))
    def reproject(self, destCRS):
        return reproject(self.crs, self.extent, destCRS)

@functools.total_ordering
class Extent(object):
    def __init__(self, xmin, ymin, xmax, ymax):
        if xmin > xmax:
            raise ExtentRangeError(
                    "Invalid Extent: xmin must be less than xmax. " +
                    "(xmin={xmin}, xmax={xmax})".format(xmin=xmin,xmax=xmax))
        if ymin > ymax:
            raise ExtentRangeError(
                    "Invalid Extent: ymin must be less than ymax. " +
                    "(ymin={ymin}, ymax={ymax})".format(ymin=ymin,ymax=ymax))
        self.xmin = float(xmin)
        self.ymin = float(ymin)
        self.xmax = float(xmax)
        self.ymax = float(ymax)

    @property
    def width(self):
        return self.xmax - self.xmin

    @property
    def height(self):
        return self.ymax - self.ymin

    @property
    def area(self):
        return self.width * self.height

    @property
    def minExtent(self):
        return min(self.width, self.height)

    @property
    def maxExtent(self):
        return max(self.width, self.height)

    @property
    def isEmpty(self):
        return self.area == 0

    def interiorIntersects(self, other):
        return (
                not (other.xmax <= self.xmin or
                    other.xmin >= self.xmax) and
                not (other.ymax <= self.ymin or
                    other.ymin >= self.ymax))

    def intersects(self, first, second = None):
        if second is None:
            other = first
            return (
                    not (other.xmax < self.xmin or
                        other.xmin > self.xmax) and
                    not (other.ymax < self.ymin or
                        other.ymin > self.ymax))
        else:
            x, y = first, second
            return (self.xmin <= x <= self.xmax and
                    self.ymin <= y <= self.ymax)

    def contains(self, first, second = None):
        if second is None:
            other = first
            if (self.xmin == 0 and self.xmax == 0 and
                    self.ymin == 0 and self.ymax == 0):
                return False
            return (
                    other.xmin >= self.xmin and
                    other.ymin >= self.ymin and
                    other.xmax <= self.xmax and
                    other.ymax <= self.ymax)
        else:
            x, y = first, second
            return (self.xmin < x < self.xmax and
                    self.ymin < y < self.ymax)

    def covers(self, first, second = None):
        if second is None:
            other = first
            return self.contains(other)
        else:
            x, y = first, second
            return self.intersects(x, y)

    def distance(self, other):
        if self.intersects(other):
            return 0
        if self.xmax < other.xmin:
            dx = other.xmin - self.xmax
        elif self.xmin > other.xmax:
            dx = self.xmin - other.xmax
        else:
            dx = 0.0

        if self.ymax < other.ymin:
            dy = other.ymin - self.ymax
        elif self.ymin > other.ymax:
            dy = self.ymin - other.ymax
        else:
            dy = 0.0

        if dx == 0.0:
            return dy
        elif dy == 0.0:
            return dx
        else:
            return math.sqrt(dx ** 2 + dy ** 2)

    def intersection(self, other):
        xminNew = max(self.xmin, other.xmin)
        yminNew = max(self.ymin, other.ymin)
        xmaxNew = min(self.xmax, other.xmax)
        ymaxNew = min(self.ymax, other.ymax)

        if xminNew <= xmaxNew and yminNew <= ymaxNew:
            return Extent(xminNew, yminNew, xmaxNew, ymaxNew)
        else:
            return None

    def __and__(self, other):
        return self.intersection(other)

    def buffer(self, d):
        return Extent(
                self.xmin - d, self.ymin - d,
                self.xmax + d, self.ymax + d)

    def __lt__(self, other):
        result = cmp(self.ymin, other.ymin)
        if result != 0:
            return result < 0
        result = cmp(self.xmin, other.xmin)
        if result != 0:
            return result < 0
        result = cmp(self.ymax, other.ymax)
        if result != 0:
            return result < 0
        result = cmp(self.xmax, other.xmax)
        if result != 0:
            return result < 0

    def combine(self, other):
        return Extent(
                min(self.xmin, other.xmin),
                min(self.ymin, other.ymin),
                max(self.xmax, other.xmax),
                max(self.ymax, other.ymax))

    def expandToInclude(self, first, second = None):
        if second is None:
            other = first
            return self.combine(other)
        else:
            x, y = first, second
            return Extent(
                min(self.xmin, x),
                min(self.ymin, y),
                max(self.xmax, x),
                max(self.ymax, y))

    def expandBy(self, deltaX, deltaY = None):
        if deltaY is None:
            deltaY = deltaX
        return Extent(
                self.xmin - deltaX,
                self.ymin - deltaY,
                self.xmax + deltaX,
                self.ymax + deltaY)

    def translate(self, deltaX, deltaY):
        return Extent(
                self.xmin + deltaX,
                self.ymin + deltaY,
                self.xmax + deltaX,
                self.ymax + deltaY)

    def __eq__(self, other):
        if not isinstance(other, Extent):
            return False
        return (self.xmin == other.xmin and self.ymin == other.ymin and
                self.xmax == other.xmax and self.ymax == other.ymax)

    def __hash__(self):
        return hash((self.xmin, self.ymin, self.xmax, self.ymax))

    def __str__(self):
        return "Extent({xmin}, {ymin}, {xmax}, {ymax})".format(
                xmin = self.xmin, ymin = self.ymin,
                xmax = self.xmax, ymax = self.ymax)

    @staticmethod
    def fromString(self, s):
        xmin,ymin,xmax,ymax = map(lambda word:float(word), s.split(","))
        return Extent(xmin,ymin,xmax,ymax)
