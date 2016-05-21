from __future__ import absolute_import

from geotrellis.raster.render.ColorMap import ColorMap
from geotrellis.raster.render.package_scala import RGBA, red, green, blue, alpha, unzipRGB
from geotrellis.raster.histogram.Histogram import Histogram

class ColorRamp(object):
    def __init__(self, *colors):
        if len(colors) == 1 and isinstance(colors[0], list):
            colors = colors[0]
        self.colors = colors

    def __eq__(self, other):
        if not isinstance(other, ColorRamp):
            return False
        return self.colors == other.colors

    def __hash__(self):
        return hash(tuple(self.colors))

    @property
    def numStops(self):
        return len(self.colors)

    def stops(self, requestedNumStops):
        if requestedNumStops < self.numStops:
            return ColorRamp(ColorRamp.spread(self.colors, requestedNumStops))
        elif requestedNumStops > self.numStops:
            return ColorRamp(ColorRamp.chooseColors(self.colors, requestedNumStops))
        else:
            return self

    def setAlphaGradient(self, start = 0, stop = 0xFF):
        chosen = ColorRamp.chooseColors([start, stop], len(self.colors))
        alphas = map(lambda c: alpha(c), chosen)

        def func(t):
            color, a = t
            r, g, b = unzipRGB(color)
            return RGBA(r, g, b, a).int
        newColors = map(func, zip(colors, alphas))
        return ColorRamp(newColors)

    def setAlpha(self, a):
        def func(color):
            r, g, b = unzipRGB(color)
            return RGBA(r, g, b, a).int
        newColors = map(func, colors)
        return ColorRamp(newColors)

    def toColorMap(self, breaks, options = ColorMap.Options.DEFAULT):
        if isinstance(breaks, Histogram):
            histogram = breaks
            return ColorMap.fromQuantileBreaks(histogram, self, options)
        else:
            return ColorMap.applyStatic(breaks, self, options)

    def toColorMapInt(self, breaks, options = ColorMap.Options.DEFAULT):
        if isinstance(breaks, Histogram):
            histogram = breaks
            return ColorMap.fromQuantileBreaksInt(histogram, self, options)
        else:
            return ColorMap.applyStaticInt(breaks, self, options)

    def toColorMapDouble(self, breaks, options = ColorMap.Options.DEFAULT):
        if isinstance(breaks, Histogram):
            histogram = breaks
            return ColorMap.fromQuantileBreaksDouble(histogram, self, options)
        else:
            return ColorMap.applyStaticDouble(breaks, self, options)

    @staticmethod
    def spread(colors, n):
        if len(colors) == n: return colors

        colors2 = [0] * n
        colors2[0] = colors[0]

        b = n - 1
        color = len(colors) - 1
        for i in xrange(1, n):
            colors2[i] = colors[int(round(float(i)*color/b))]
        return colors2

    @staticmethod
    def chooseColors(c, numColors):
        def func(masker, count):
            hues = map(masker, c)
            mult = len(c) - 1
            denom = count - 1

            if count < 2:
                return [hues[0]]

            ranges = [0] * count
            for i in xrange(0, count):
                j = (i * mult) / denom
                if j < mult:
                    value = _blend(hues[j], hues[j+1], (i*mult) % denom, denom)
                else:
                    value = hues[j]
                ranges[i] = value
            return ranges
        return _getColorSequence(numColors, func)

def _blend(start, end, numerator, denominator):
    return start + ( ((end - start) * numerator) / denominator )

def _getColorSequence(n, getRanges):
    unzipR = lambda color: red(color)
    unzipG = lambda color: green(color)
    unzipB = lambda color: blue(color)
    unzipA = lambda color: alpha(color)
    rs = getRanges(unzipR, n)
    gs = getRanges(unzipG, n)
    bs = getRanges(unzipB, n)
    as_ = getRanges(unzipA, n)

    theColors = [0] * n
    for i in xrange(0, n):
        theColors[i] = RGBA(rs[i], gs[i], bs[i], as_[i]).int
    return theColors
