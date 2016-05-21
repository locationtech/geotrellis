from __future__ import absolute_import

from geotrellis.raster.package_scala import intToByte

import array
import zlib
import struct

def _i1(value):
    return struct.pack("!B", value & (2**8-1))
def _i4(value):
    return struct.pack("!I", value & (2**32-1))

class Chunk(object):
    def __init__(self, chunkType):
        self.chunkType = chunkType
        self.output = array.array('b')
        #self.writeInt(chunkType)

    def __eq__(self, other):
        if not isinstance(other, Chunk):
            return False
        return self.chunkType == other.chunkType

    def __hash__(self):
        hash((self.chunkType,))

    def writeInt(self, i):
        #arr = array.array('b', [
        #        intToByte(i >> 24),
        #        intToByte(i >> 16),
        #        intToByte(i >> 8),
        #        intToByte(i)
        #    ])
        #self.output.fromstring(arr.tostring())
        string = struct.pack("!I", i & (2**32-1))
        self.output.fromstring(string)

    def writeByte(self, b):
        string = struct.pack("!B", b & (2**8-1))
        self.output.fromstring(string)

    def writeTo(self, output):
        string = self.output.tostring()
        _writeIntTo(len(string), output)
        _writeStringTo(self.chunkType + string, output)
        crc = zlib.crc32(self.chunkType + string) % (1<<32)
        _writeIntTo(crc, output)

def _writeIntTo(i, out):
    string = struct.pack("!I", i & (2**32-1))
    _writeStringTo(string, out)

def _writeBytesTo(arr, out):
    string = arr.tostring()
    _writeStringTo(string, out)

def _writeStringTo(string, out):
    #towrite = len(string)
    #while towrite > 0:
    #    towrite -= out.write(string[-towrite:])
    out.write(string)
