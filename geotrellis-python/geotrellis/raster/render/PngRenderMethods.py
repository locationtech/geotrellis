from __future__ import absolute_import

from geotrellis.raster.render.png.PngColorEncoding import PngColorEncoding, RgbaPngEncoding
from geotrellis.raster.render.png.PngEncoder import PngEncoder
from geotrellis.raster.render.png.Filter import PaethFilter
from geotrellis.raster.render.png.Settings import Settings
from geotrellis.raster.render.ImageFormats import Png
from geotrellis.raster.render.ColorMap import ColorMap, IntColorMap
from geotrellis.raster.render.ColorRamp import ColorRamp

def renderPng(raster, first = None):
    if first is None:
        return renderPng(raster, RgbaPngEncoding)
    if isinstance(first, PngColorEncoding):
        colorEncoding = first
        encoder = PngEncoder(Settings(colorEncoding, PaethFilter))
        bytesarray = encoder.writeByteArray(raster)
        return Png.arrayByteToPng(bytesarray)
    if isinstance(first, ColorMap):
        colorMap = first
        colorEncoding = PngColorEncoding.applyStatic(colorMap.colors, colorMap.options.noDataColor, colorMap.options.fallbackColor)
        convertedColorMap = colorEncoding.convertColorMap(colorMap)
        return _renderPng(raster, colorEncoding, convertedColorMap)
    if isinstance(first, ColorRamp):
        colorRamp = first
        if raster.cellType.isFloatingPoint:
            histogram = raster.histogram
            quantileBreaks = histogram.quantileBreaks(colorRamp.numStops)
            return renderPng(IntColorMap(dict(zip(quantileBreaks, colorRamp.colors))).cache(histogram))
        else:
            histogram = raster.histogramDouble
            return renderPng(ColorMap.fromQuantileBreaks(histogram, colorRamp))

def _renderPng(raster, colorEncoding, colorMap):
    encoder = PngEncoder(Settings(colorEncoding, PaethFilter))
    bytesarray = encoder.writeByteArray(colorMap.render(raster))
    return Png.arrayByteToPng(bytesarray)
