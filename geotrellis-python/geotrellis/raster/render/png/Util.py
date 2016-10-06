from __future__ import absolute_import

from geotrellis.raster.package_scala import intToByte, intToUByte

def initByteBuffer32(bb, d, size):
    for z in d[:size]:
        bb.append(intToByte(z >> 24))
        bb.append(intToByte(z >> 16))
        bb.append(intToByte(z >> 8))
        bb.append(intToByte(z))

def initByteBuffer24(bb, d, size):
    for z in d[:size]:
        bb.append(intToByte(z >> 16))
        bb.append(intToByte(z >> 8))
        bb.append(intToByte(z))

def initByteBuffer16(bb, d, size):
    for z in d[:size]:
        bb.append(intToByte(z >> 8))
        bb.append(intToByte(z))

def initByteBuffer8(bb, d, size):
    for z in d[:size]:
        bb.append(intToByte(z))

def initUByteBuffer32(bb, d, size):
    for z in d[:size]:
        bb.append(intToUByte(z >> 24))
        bb.append(intToUByte(z >> 16))
        bb.append(intToUByte(z >> 8))
        bb.append(intToUByte(z))

def initUByteBuffer24(bb, d, size):
    for z in d[:size]:
        bb.append(intToUByte(z >> 16))
        bb.append(intToUByte(z >> 8))
        bb.append(intToUByte(z))

def initUByteBuffer16(bb, d, size):
    for z in d[:size]:
        bb.append(intToUByte(z >> 8))
        bb.append(intToUByte(z))

def initUByteBuffer8(bb, d, size):
    for z in d[:size]:
        bb.append(intToUByte(z))
