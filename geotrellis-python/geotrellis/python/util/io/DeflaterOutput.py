from __future__ import absolute_import

import array
import zlib

class DeflaterOutput(object):

    BEST_SPEED = 1

    def __init__(self, arr, level = 6):
        self.arr = arr
        self.level = level

    def write(self, b):
        if isinstance(b, int):
            buf = array.array('b', [b & 0xff])
            string = buf.tostring()
        elif isinstance(b, array.array):
            string = b.tostring()
        elif isinstance(b, str):
            string = b
        else:
            raise Exception("unsupported input; type: {tp}, value: {b}".format(
                tp=type(b), b=b))

        deflated = zlib.compress(string, self.level)
        self.arr.fromstring(deflated)
