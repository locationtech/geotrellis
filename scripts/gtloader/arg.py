"""
Code dealing with the ARG format.
"""
import math
import struct

import log
from datatypes import *

class ArgWriter():
    """ 
    Writes an arg in the 
    given input datatype (such as bit, int8, float32, etc)

    buf is a file-like object with a method write(bytearray)
    datatype 
    verify is a boolean. If 'verify' is set to true and
          data would be truncated, the operation will stop
          If 'verify' is set to false the write operation
          will truncate all datavalues to fit into the given
          datatype
    """
    def __init__(self, buf, datatype, verify = True):
        self.buf = buf
        self.datatype = datatype
        self.verify = verify

        self.sfmt = to_struct_fmt(self.datatype)
        if not self.verify:
            self.truncate = self.get_truncator()

        if not self.sfmt:
            log.error("Couldn't find a python format for '%s'" % self.datatype)

    def write(self,values):
        """
        Writes values to the buffer.

        values is a list-like structure with the data to write
        """
        outputfmt = '>' + (self.sfmt * len(values))
        try:
            self.buf.write(struct.pack(outputfmt, *values))
        except Exception, e:
            if self.verify:
                for v in values:
                    #TODO: Handle bit types specially

                    # Pack and unpack, see if we get the same value
                    failed = False
                    nv = None
                    try:
                        nv = struct.unpack('>' + self.sfmt,
                                           struct.pack('>' + self.sfmt, v))[0]
                    except struct.error, e:
                        print e
                        failed = True

                    # Note, is both nv and v are nan, check will fail:
                    # nan != nan, but in our case we want that to be
                    # true so we explicitly check for it here
                    if failed or \
                       (nv != v and
                       (not math.isnan(nv) and math.isnan(v))):
                         log.error('Verification failed. Trying to '\
                              'convert to %s resuled in: %s -> %s' %\
                              (self.datatype, v, nv))
            else: # Just make it work
                for i in xrange(0,len(values)):
                    self.buf.write(struct.pack('>' + self.sfmt, self.truncate(values[i])))

    def get_truncator(self):
        """ Return a function that is able to truncate data
        based on the input type."""
        def truncate_pow(exp, val):
            """ Given a signed interger datatype with 'exp' bits,
            truncate val so that it is in the interval:
                -2**(exp-1) <= val <= 2**(exp-1) - 1
            """
            if math.isnan(float(val)):
                return -2**(exp-1)

            val = int(val)
            if val >= 2**(exp-1) - 1:
                return 2**(exp -1) - 1
            elif val < -2**(exp - 1):
                return -2**(exp - 1)
            else:
                return val

        if self.datatype == 'bit':
            return lambda val: int(val) & 0x1
        elif self.datatype == 'int8':
            return lambda val: truncate_pow(8, val)
        elif self.datatype == 'int16':
            return lambda val: truncate_pow(16, val)
        elif self.datatype == 'int32':
            return lambda val: truncate_pow(32, val)
        elif self.datatype == 'int64':
            return lambda val: truncate_pow(64, val)
        else:
            return lambda val: val
