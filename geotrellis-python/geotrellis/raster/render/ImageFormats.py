from __future__ import absolute_import

class ImageFormat(object): pass

class Jpg(ImageFormat):
    def __init__(self, bytesarray):
        self.bytes = bytesarray

    def __eq__(self, other):
        if not isinstance(other, Jpg):
            return False
        return self.bytes == other.bytes

    def __hash__(self):
        return hash(self.bytes.tostring())

    @staticmethod
    def jpgToArrayByte(jpg):
        return jpg.bytes

    @staticmethod
    def arrayByteToJpg(arr):
        return Jpg(arr)

class Png(ImageFormat):
    def __init__(self, bytesarray):
        self.bytes = bytesarray

    def __eq__(self, other):
        if not isinstance(other, Png):
            return False
        return self.bytes == other.bytes

    def __hash__(self):
        return hash(self.bytes.tostring())

    @staticmethod
    def pngToArrayByte(png):
        return png.bytes

    @staticmethod
    def arrayByteToPng(arr):
        return Png(arr)
