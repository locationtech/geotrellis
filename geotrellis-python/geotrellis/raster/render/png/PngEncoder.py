from __future__ import absolute_import
from geotrellis.raster.render.png.Chunk import Chunk
from geotrellis.raster.render.png.Filter import PaethFilter
from geotrellis.raster.render.png.Util import (
        initByteBuffer32,
        initByteBuffer24,
        initByteBuffer16,
        initByteBuffer8,
        initUByteBuffer32,
        initUByteBuffer24,
        initUByteBuffer16,
        initUByteBuffer8)
from geotrellis.raster.render.png.PngColorEncoding import (
        GreyPngEncoding, RgbPngEncoding, IndexedPngEncoding)
from geotrellis.python.util.io.DeflaterOutput import DeflaterOutput
from geotrellis.raster.render.png.Util import (
        initByteBuffer32,
        initByteBuffer24,
        initByteBuffer16,
        initByteBuffer8,
        initUByteBuffer32,
        initUByteBuffer24,
        initUByteBuffer16,
        initUByteBuffer8)
from geotrellis.raster.package_scala import intToByte, intToUByte
import array
import io
import png

class PngEncoder(object):
    def __init__(self, settings):
        self.settings = settings
        self.FILTER = settings.filter.n
        self.DEPTH = settings.colorType.depth
        self.SHIFT = (self.DEPTH - 1) * 8

    def __eq__(self, other):
        if not isinstance(other, PngEncoder):
            return False
        return self.settings == other.settings

    def __hash__(self):
        return hash((self.settings,))

    def createByteBuffer(self, raster):
        size = raster.size
        data = raster.toArray()
        bb = array.array('B')
        if self.DEPTH == 4:
            initUByteBuffer32(bb, data, size)
        elif self.DEPTH == 3:
            initUByteBuffer24(bb, data, size)
        elif self.DEPTH == 2:
            initUByteBuffer16(bb, data, size)
        elif self.DEPTH == 1:
            initUByteBuffer8(bb, data, size)
        else:
            raise Exception("unsupported depth: {d}".format(d=self.DEPTH))
        return bb

    def writePixelDataNoFilter(self, output, raster):
        cols = raster.cols
        size = cols * raster.rows
        bb = self.createByteBuffer(raster)
        byteWidth = cols * self.DEPTH
        yspan = 0
        while yspan < size:
            start = yspan * self.DEPTH
            end = min(start + byteWidth, len(bb))
            currLine = bb[start:end]
            yield currLine
            yspan += cols

    def writeOutputStream(self, output, raster):
        colorType = self.settings.colorType
        rgbs = colorType.rgbs
        alphas = colorType.as_

        def paletteMapper(t):
            rgb, a = t
            result = (intToUByte(rgb >> 16), intToUByte(rgb >> 8), intToUByte(rgb), intToUByte(a))
            return result
        writer = png.Writer(
                width=raster.cols, height=raster.rows, 
                palette = map(paletteMapper, zip(rgbs, alphas))
                )
        writer.write_passes(output, self.writePixelDataNoFilter(output, raster))

    def writeByteArray(self, raster):
        output = io.BytesIO()
        self.writeOutputStream(output, raster)
        return output.getvalue()

    def writePath(self, path, raster):
        with open(path) as f:
            self.writeOutputStream(f, raster)
