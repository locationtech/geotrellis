from __future__ import absolute_import

#from geotrellis.raster.render.ColorRamp import ColorRamp
from geotrellis.raster.ArrayTile import ArrayTile
from geotrellis.raster.CellType import IntCellType
from geotrellis.raster.package_scala import isNoData, d2i, i2d
from geotrellis.python.util.utils import fullname

class ClassBoundaryType(object):
    def __eq__(self, other):
        return isinstance(other, type(self))
    def __hash__(self):
        return hash(fullname(type(self)))

class _GreaterThan(ClassBoundaryType): pass
GreaterThan = _GreaterThan()

class _GreaterThanOrEqualTo(ClassBoundaryType): pass
GreaterThanOrEqualTo = _GreaterThanOrEqualTo()

class _LessThan(ClassBoundaryType): pass
LessThan = _LessThan()

class _LessThanOrEqualTo(ClassBoundaryType): pass
LessThanOrEqualTo = _LessThanOrEqualTo()

class _Exact(ClassBoundaryType): pass
Exact = _Exact()

class Options(object):

    @staticmethod
    def classBoundaryTypeToOptions(classBoundaryType):
        return Options(classBoundaryType)

    def __init__(self,
            classBoundaryType = LessThanOrEqualTo,
            noDataColor = 0x00000000,
            fallbackColor = 0x00000000,
            strict = False):
        self.classBoundaryType = classBoundaryType
        self.noDataColor = noDataColor
        self.fallbackColor = fallbackColor
        self.strict = strict

    def __eq__(self, other):
        if not isinstance(other, Options):
            return False
        return (self.classBoundaryType == other.classBoundaryType and
                self.noDataColor == other.noDataColor and
                self.fallbackColor == other.fallbackColor and
                self.strict == other.strict)

    def __hash__(self):
        return hash(
                self.classBoundaryType,
                self.noDataColor,
                self.fallbackColor,
                self.strict)

    def copy(self,
                classBoundaryType = None,
                noDataColor = None,
                fallbackColor = None,
                strict = None):
        classBoundaryType = classBoundaryType if classBoundaryType is not None else self.classBoundaryType
        noDataColor = noDataColor if noDataColor is not None else self.noDataColor
        fallbackColor = fallbackColor if fallbackColor is not None else self.fallbackColor
        strict = strict if strict is not None else self.strict
        
        return Options(
                classBoundaryType,
                noDataColor,
                fallbackColor,
                strict)

Options.DEFAULT = Options()

class ColorMap(object):

    Options = Options

    _opaque = None
    _grey = None

    @property
    def colors(self):
        pass

    @property
    def opaque(self):
        if self._opaque is None:
            self._init_opaque_grey()
        return self._opaque

    @property
    def grey(self):
        if self._grey is None:
            self._init_opaque_grey()
        return self._grey

    def _init_opaque_grey(self):
        opaque = True
        grey = True
        for c in self.colors:
            opaque = opaque and RGBA(c).isOpaque
            grey = grey and RGBA(c).isGrey
        self._opaque = opaque
        self._grey = grey

    def render(self, tile):
        result = ArrayTile.empty(IntCellType, tile.cols, tile.rows)
        if tile.cellType.isFloatingPoint:
            def func(col, row, z):
                result.setDouble(col, row, self.mapDouble(z))
            tile.foreachDouble(func)
        else:
            def func(col, row, z):
                result.set(col, row, self.map(z))
            tile.foreach(func)
        return result

    @staticmethod
    def applyStatic(*args):
        breaksToColors, options = _breaksToColors_options(args)
        if len(breaksToColors) == 0:
            # cannot determine empty dict's key type, default to int
            return IntColorMap(breaksToColors, options)
        firstKey = breaksToColors.keys()[0]
        if isinstance(firstkey, float):
            return DoubleColorMap(breaksToColors, options)
        else:
            return IntColorMap(breaksToColors, options)

    @staticmethod
    def applyStaticInt(*args):
        breaksToColors, options = _breaksToColors_options(args)
        return IntColorMap(breaksToColors, options)

    @staticmethod
    def applyStaticDouble(*args):
        breaksToColors, options = _breaksToColors_options(args)
        return DoubleColorMap(breaksToColors, options)

    @staticmethod
    def fromQuantileBreaks(histogram, colorRamp, options = Options.DEFAULT):
        if histogram.T == int:
            return ColorMap.fromQuantileBreaksInt(histogram, colorRamp, options)
        else:
            return ColorMap.fromQuantileBreaksDouble(histogram, colorRamp, options)

    @staticmethod
    def fromQuantileBreaksInt(histogram, colorRamp, options = Options.DEFAULT):
        return ColorMap.applyStaticInt(histogram.quantileBreaks(colorRamp.numStops), colorRamp.colors, options)

    @staticmethod
    def fromQuantileBreaksDouble(histogram, colorRamp, options = Options.DEFAULT):
        cm = ColorMap.applyStaticDouble(histogram.quantileBreaks(colorRamp.numStops), colorRamp.colors, options)
        cbt = options.classBoundaryType

        if cbt == GreaterThan or cbt == GreaterThanOrEqualTo:
            return cm.withFallbackColor(colorRamp.colors(0))

        if cbt == LessThan or cbt == LessThanOrEqualTo:
            return cm.withFallbackColor(colorRamp.colors(len(colorRamp.colors) - 1))
        if cbt == Exact:
            return cm
        else:
            raise Exception("unknown classBoundaryType {cbt}".format(cbt=cbt))

    @staticmethod
    def fromString(string):
        split = map(lambda word: word.strip().split(':'), string.split(';'))
        try:
            limits = map(lambda pair: int(pair[0]), split)
            colors = map(lambda pair: int(pair[1], 16), split)
            if len(limits) != len(colors): raise Exception("len(limits) != len(colors)")
            return ColorMap.applyStaticInt(limits, colors)
        except Exception:
            return None

    @staticmethod
    def fromStringDouble(string):
        split = map(lambda word: word.strip().split(':'), string.split(';'))
        try:
            limits = map(lambda pair: float(pair[0]), split)
            colors = map(lambda pair: int(pair[1], 16), split)
            from geotrellis.raster.render.ColorRamp import ColorRamp
            return ColorMap.applyStaticDouble(limits, ColorRamp(colors))
        except Exception as e:
            return None

def _breaksToColors_options(args):
    from geotrellis.raster.render.ColorRamp import ColorRamp
    if len(args) == 2 and isinstance(args[1], Options):
        breaksToColors = args[0]
        options = args[1]
    elif len(args) > 1 and isinstance(args[1], ColorRamp):
        breaks = args[0]
        colorRamp = args[1]
        colors = colorRamp.stops(len(breaks)).colors

        breaksToColors = dict(zip(breaks, colors))
        options = Options.DEFAULT if len(args) == 2 else args[2]
    elif len(args) == 1 and isinstance(args[0], dict):
        breaksToColors = args[0]
        options = Options.DEFAULT
    else:
        breaksToColors = dict(args)
        options = Options.DEFAULT
    return (breaksToColors, options)
        

class IntColorMap(ColorMap):
    _orderedBreaks = None
    _orderedColors = None

    def __init__(self, breaksToColors, options = Options.DEFAULT):
        self._breaksToColors = breaksToColors
        self.options = options
        self._len = len(breaksToColors)

        cbt = options.classBoundaryType
        if cbt == LessThan:
            zCheck = lambda z, i: z >= self.orderedBreaks[i]
        elif cbt == LessThanOrEqualTo:
            zCheck = lambda z, i: z > self.orderedBreaks[i]
        elif cbt == GreaterThan:
            zCheck = lambda z, i: z <= self.orderedBreaks[i]
        elif cbt == GreaterThanOrEqualTo:
            zCheck = lambda z, i: z < self.orderedBreaks[i]
        elif cbt == Exact:
            zCheck = lambda z, i: z != self.orderedBreaks[i]
        self._zCheck = zCheck

    @property
    def orderedBreaks(self):
        if self._orderedBreaks is not None:
            return self._orderedBreaks
        cbt = self.options.classBoundaryType
        breaks = self._breaksToColors.keys()
        if cbt == LessThan or cbt == LessThanOrEqualTo:
            breaks.sort()
        elif cbt == GreaterThan or cbt == GreaterThanOrEqualTo:
            breaks.sort(reverse = True)
        self._orderedBreaks = breaks
        return self._orderedBreaks

    @property
    def orderedColors(self):
        if self._orderedColors is not None:
            return self._orderedColors
        colors = map(self._breaksToColors.get, self.orderedBreaks)
        self._orderedColors = colors
        return self._orderedColors

    @property
    def colors(self):
        return self.orderedColors

    def map(self, z):
        if (isNoData(z)):
            return self.options.noDataColor
        i = 0
        while i < self._len and self._zCheck(z, i): i += 1
        if i != self._len:
            self.orderedColors[i]
        elif self.options.strict:
            raise Exception("Value {z} did not have an associated color and break".format(z=z))
        else:
            return self.options.fallbackColor

    def mapDouble(self, z):
        return self.map(d2i(z))

    def mapColors(self, f):
        newBreaksToColors = dict(map(lambda t: (t[0], f(t[1])), self._breaksToColors.items()))
        return IntColorMap(newBreaksToColors, self.options)

    def mapColorsToIndex(self):
        zippedWithIndex = map(tuple, map(reversed, enumerate(self.orderedBreaks)))
        newBreaksToColors = dict(zippedWithIndex)
        return IntColorMap(newBreaksToColors, self.options)

    def withNoDataColor(self, color):
        return IntColorMap(self._breaksToColors, self.options.copy(noDataColor = color))

    def withFallbackColor(self, color):
        return IntColorMap(self._breaksToColors, self.options.copy(fallbackColor = color))

    def withBoundaryType(self, classBoundaryType):
        return IntColorMap(self._breaksToColors, self.options.copy(classBoundaryType = classBoundaryType))

    def cache(self, h):
        ch = h.mutable
        h.foreachValue(lambda z: ch.setItem(z, self.map(z)))
        return IntCachedColorMap(self.orderedColors, ch, self.options)

class IntCachedColorMap(ColorMap):
    def __init__(self, colors, h, options):
        self.colors = colors
        self.h = h
        self.options = options
        self.noDataColor = options.noDataColor

    def map(self, z):
        if isNoData(z):
            return self.noDataColor
        else:
            return self.h.itemCount(z)

    def mapDouble(self, z):
        return self.map(d2i(z))

    def mapColors(self, f):
        ch = self.h.mutable
        h.foreachValue(lambda z: ch.setItem(z, f(h.itemCount(z))))
        return IntCachedColorMap(self.colors, ch, self.options)

    def mapColorsToIndex(self):
        colorIndexMap = dict(enumerate(self.colors))
        ch = self.h.mutable
        h.foreachValue(lambda z: ch.setItem(z, colorIndexMap[h.itemCount(z)]))
        return IntCachedColorMap(range(0, len(self.colors)+1), ch, self.options)

    def withNoDataColor(self, color):
        return IntCachedColorMap(self.colors, self.h, self.options.copy(noDataColor = color))

    def withFallbackColor(self, color):
        return IntCachedColorMap(self.colors, self.h, self.options.copy(fallbackColor = color))

    def withBoundaryType(self, classBoundaryType):
        return IntCachedColorMap(self.colors, self.h, self.options.copy(classBoundaryType = classBoundaryType))

class DoubleColorMap(ColorMap):
    _orderedBreaks = None
    _orderedColors = None

    def __init__(self, breaksToColors, options = Options.DEFAULT):
        self._breaksToColors = breaksToColors
        self.options = options
        self._len = len(breaksToColors)

        cbt = options.classBoundaryType
        if cbt == LessThan:
            zCheck = lambda z, i: z >= self.orderedBreaks[i]
        elif cbt == LessThanOrEqualTo:
            zCheck = lambda z, i: z > self.orderedBreaks[i]
        elif cbt == GreaterThan:
            zCheck = lambda z, i: z <= self.orderedBreaks[i]
        elif cbt == GreaterThanOrEqualTo:
            zCheck = lambda z, i: z < self.orderedBreaks[i]
        elif cbt == Exact:
            zCheck = lambda z, i: z != self.orderedBreaks[i]
        self._zCheck = zCheck

    @property
    def orderedBreaks(self):
        if self._orderedBreaks is not None:
            return self._orderedBreaks
        cbt = self.options.classBoundaryType
        breaks = self._breaksToColors.keys()
        if cbt == LessThan or cbt == LessThanOrEqualTo:
            breaks.sort()
        elif cbt == GreaterThan or cbt == GreaterThanOrEqualTo:
            breaks.sort(reverse = True)
        self._orderedBreaks = breaks
        return self._orderedBreaks

    @property
    def orderedColors(self):
        if self._orderedColors is not None:
            return self._orderedColors
        colors = map(self._breaksToColors.get, self.orderedBreaks)
        self._orderedColors = colors
        return self._orderedColors

    @property
    def colors(self):
        return self.orderedColors

    def map(self, z):
        return self.map(i2d(z))

    def mapDouble(self, z):
        if (isNoData(z)):
            return self.options.noDataColor
        i = 0
        while i < self._len and self._zCheck(z, i): i += 1
        if i != self._len:
            return self.orderedColors[i]
        elif self.options.strict:
            raise Exception("Value {z} did not have an associated color and break".format(z=z))
        else:
            return self.options.fallbackColor

    def mapColors(self, f):
        newBreaksToColors = dict(map(lambda t: (t[0], f(t[1])), self._breaksToColors.items()))
        return DoubleColorMap(newBreaksToColors, self.options)

    def mapColorsToIndex(self):
        zippedWithIndex = map(tuple, map(reversed, enumerate(self.orderedBreaks)))
        newBreaksToColors = dict(zippedWithIndex)
        return DoubleColorMap(newBreaksToColors, self.options)

    def withNoDataColor(self, color):
        return DoubleColorMap(self._breaksToColors, self.options.copy(noDataColor = color))

    def withFallbackColor(self, color):
        return DoubleColorMap(self._breaksToColors, self.options.copy(fallbackColor = color))

    def withBoundaryType(self, classBoundaryType):
        return DoubleColorMap(self._breaksToColors, self.options.copy(classBoundaryType = classBoundaryType))
