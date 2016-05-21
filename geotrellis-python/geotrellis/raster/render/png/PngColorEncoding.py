from __future__ import absolute_import

from geotrellis.raster.render.package_scala import alpha, toARGB, isOpaque, isGrey

class PngColorEncoding(object):
    def __init__(self, n, depth):
        self.n = n
        self.depth = depth

    @staticmethod
    def applyStatic(colors, noDataColor, fallbackColor):
        length = len(colors)
        if length <= 254:
            rgbs = [0] * 256
            as_ = [0] * 256
            
            for i in xrange(0, length):
                c = colors[i]
                rgbs[i] = toARGB(c)
                as_[i] = alpha(c)
            rgbs[254] = toARGB(fallbackColor)
            as_[254] = alpha(fallbackColor)

            rgbs[255] = toARGB(noDataColor)
            as_[255] = alpha(noDataColor)
            return IndexedPngEncoding(rgbs, as_)
        else:
            opaque = True
            grey = True

            for c in colors + [fallbackColor, noDataColor]:
                opaque = opaque and isOpaque(c)
                grey = grey and isGrey(c)

            if grey and opaque:
                return GreyPngEncoding(noDataColor)
            elif opaque:
                return RgbPngEncoding(noDataColor)
            elif grey:
                return GreyaPngEncoding
            else:
                return RgbaPngEncoding

class GreyPngEncoding(PngColorEncoding):
    def __init__(self, transparent = None):
        PngColorEncoding.__init__(self, 0, 1)
        self.transparent = transparent

    def __eq__(self, other):
        if not isinstance(other, GreyPngEncoding):
            return False
        return self.transparent == other.transparent

    def __hash__(self):
        return hash((self.n, self.depth, self.transparent))

    def convertColorMap(self, colorMap):
        return colorMap.mapColors(lambda c: c.blue)

class RgbPngEncoding(PngColorEncoding):
    def __init__(self, transparent = None):
        PngColorEncoding.__init__(self, 2, 3)
        self.transparent = transparent

    def __eq__(self, other):
        if not isinstance(other, RgbPngEncoding):
            return False
        return self.transparent == other.transparent

    def __hash__(self):
        return hash((self.n, self.depth, self.transparent))

    def convertColorMap(self, colorMap):
        return colorMap.mapColors(lambda c: c >> 8)

class IndexedPngEncoding(PngColorEncoding):
    def __init__(self, rgbs, as_):
        PngColorEncoding.__init__(self, 3, 1)
        self.rgbs = rgbs
        self.as_ = as_

    def __eq__(self, other):
        if not isinstance(other, IndexedPngEncoding):
            return False
        return self.rgbs == other.rgbs and self.as_ == other.as_

    def __hash__(self):
        return hash(tuple(self.rgbs + self.as_))

    def convertColorMap(self, colorMap):
        return colorMap.mapColorsToIndex().withNoDataColor(255).withFallbackColor(254)

class _GreyaPngEncoding(PngColorEncoding):
    def __init__(self):
        PngColorEncoding.__init__(self, 4, 2)

    def __eq__(self, other):
        return isinstance(other, _GreyaPngEncoding)

    def __hash__(self):
        return hash((self.n, self.depth))

    def convertColorMap(self, colorMap):
        return colorMap.mapColors(lambda c: c & 0xffff)

GreyaPngEncoding = _GreyaPngEncoding()

class _RgbaPngEncoding(PngColorEncoding):
    def __init__(self):
        PngColorEncoding.__init__(self, 6, 4)

    def __eq__(self, other):
        return isinstance(other, _RgbaPngEncoding)

    def __hash__(self):
        return hash((self.n, self.depth))

    def convertColorMap(self, colorMap):
        return colorMap

RgbaPngEncoding = _RgbaPngEncoding()
