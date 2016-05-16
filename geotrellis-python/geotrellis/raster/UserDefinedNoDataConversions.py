from __future__ import absolute_import
from geotrellis.raster.package_scala import (INTMIN, BYTEMIN, SHORTMIN,
        intToByte, intToShort)

import math

class UserDefinedByteNoDataConversions(object):
    @property
    def userDefinedByteNoDataValue(self):
        return self._cellType.noDataValue

    # from user defined

    def udb2b(self, n):
        if n == self.userDefinedByteNoDataValue:
            return BYTEMIN
        else:
            return n

    def udb2ub(self, n):
        if n == self.userDefinedByteNoDataValue:
            return 0
        else:
            return n

    def udb2s(self, n):
        if n == self.userDefinedByteNoDataValue:
            return SHORTMIN
        else:
            return n

    def udb2us(self, n):
        if n == self.userDefinedByteNoDataValue:
            return 0
        else:
            return n

    def udb2i(self, n):
        if n == self.userDefinedByteNoDataValue:
            return INTMIN
        else:
            return n

    def udub2i(self, n):
        if n == self.userDefinedByteNoDataValue:
            return INTMIN
        else:
            return n & 0xFF

    def udb2f(self, n):
        if n == self.userDefinedByteNoDataValue:
            return float("nan")
        else:
            return float(n)

    def udub2f(self, n):
        if n == self.userDefinedByteNoDataValue:
            return float("nan")
        else:
            return float(n & 0xFF)

    def udb2d(self, n):
        if n == self.userDefinedByteNoDataValue:
            return float("nan")
        else:
            return float(n)

    def udub2d(self, n):
        if n == self.userDefinedByteNoDataValue:
            return float("nan")
        else:
            return float(n & 0xFF)

    # to user defined

    def b2udb(self, n):
        if n == BYTEMIN:
            return self.userDefinedByteNoDataValue
        else:
            return n

    def ub2udb(self, n):
        if n == 0:
            return self.userDefinedByteNoDataValue
        else:
            return n

    def s2udb(self, n):
        if n == SHORTMIN:
            return self.userDefinedByteNoDataValue
        else:
            return n

    def us2udb(self, n):
        if n == 0:
            return self.userDefinedByteNoDataValue
        else:
            return intToByte(n)

    def i2udb(self, n):
        if n == INTMIN:
            return self.userDefinedByteNoDataValue
        else:
            return intToByte(n)

    def f2udb(self, n):
        if math.isnan(n):
            return self.userDefinedByteNoDataValue
        else:
            return intToByte(int(n))

    def d2udb(self, n):
        if math.isnan(n):
            return self.userDefinedByteNoDataValue
        else:
            return intToByte(int(n))

class UserDefinedShortNoDataConversions(object):
    @property
    def userDefinedShortNoDataValue(self):
        return self._cellType.noDataValue

    # from user defined

    def uds2b(self, n):
        if n == self.userDefinedShortNoDataValue:
            return BYTEMIN
        else:
            return intToByte(n)

    def uds2ub(self, n):
        if n == self.userDefinedShortNoDataValue:
            return 0
        else:
            return intToByte(n)

    def uds2s(self, n):
        if n == self.userDefinedShortNoDataValue:
            return SHORTMIN
        else:
            return n

    def uds2us(self, n):
        if n == self.userDefinedShortNoDataValue:
            return 0
        else:
            return n

    def uds2i(self, n):
        if n == self.userDefinedShortNoDataValue:
            return INTMIN
        else:
            return n

    def udus2i(self, n):
        if n == self.userDefinedShortNoDataValue:
            return INTMIN
        else:
            return n & 0xFFFF

    def uds2f(self, n):
        if n == self.userDefinedShortNoDataValue:
            return float("nan")
        else:
            return float(n)

    def udus2f(self, n):
        if n == self.userDefinedShortNoDataValue:
            return float("nan")
        else:
            return float(n & 0xFFFF)

    def uds2d(self, n):
        if n == self.userDefinedShortNoDataValue:
            return float("nan")
        else:
            return float(n)

    def udus2d(self, n):
        if n == self.userDefinedShortNoDataValue:
            return float("nan")
        else:
            return float(n & 0xFFFF)

    # to user defined

    def b2uds(self, n):
        if n == BYTEMIN:
            return self.userDefinedShortNoDataValue
        else:
            return n

    def ub2uds(self, n):
        if n == 0:
            return self.userDefinedShortNoDataValue
        else:
            return n

    def s2uds(self, n):
        if n == SHORTMIN:
            return self.userDefinedShortNoDataValue
        else:
            return n

    def us2uds(self, n):
        if n == 0:
            return self.userDefinedShortNoDataValue
        else:
            return n

    def i2uds(self, n):
        if n == INTMIN:
            return self.userDefinedShortNoDataValue
        else:
            return intToShort(n)

    def f2uds(self, n):
        if math.isnan(n):
            return self.userDefinedShortNoDataValue
        else:
            return intToShort(int(n))

    def d2uds(self, n):
        if math.isnan(n):
            return self.userDefinedShortNoDataValue
        else:
            return intToShort(int(n))


class UserDefinedIntNoDataConversions(object):
    @property
    def userDefinedIntNoDataValue(self):
        return self._cellType.noDataValue

    # from user defined

    def udi2b(self, n):
        if n == self.userDefinedIntNoDataValue:
            return BYTEMIN
        else:
            return intToByte(n)

    def udi2ub(self, n):
        if n == self.userDefinedIntNoDataValue:
            return 0
        else:
            return intToByte(n)

    def udi2s(self, n):
        if n == self.userDefinedIntNoDataValue:
            return SHORTMIN
        else:
            return intToShort(n)

    def udi2us(self, n):
        if n == self.userDefinedIntNoDataValue:
            return 0
        else:
            return intToShort(n)

    def udi2i(self, n):
        if n == self.userDefinedIntNoDataValue:
            return INTMIN
        else:
            return n

    def udi2f(self, n):
        if n == self.userDefinedIntNoDataValue:
            return float("nan")
        else:
            return float(n)

    def udi2d(self, n):
        if n == self.userDefinedIntNoDataValue:
            return float("nan")
        else:
            return float(n)

    # to user defined

    def b2udi(self, n):
        if n == BYTEMIN:
            return self.userDefinedIntNoDataValue
        else:
            return n

    def ub2udi(self, n):
        if n == 0:
            return self.userDefinedIntNoDataValue
        else:
            return n

    def s2udi(self, n):
        if n == SHORTMIN:
            return self.userDefinedIntNoDataValue
        else:
            return n

    def us2udi(self, n):
        if n == 0:
            return self.userDefinedIntNoDataValue
        else:
            return n

    def i2udi(self, n):
        if n == INTMIN:
            return self.userDefinedIntNoDataValue
        else:
            return n

    def f2udi(self, n):
        if math.isnan(n):
            return self.userDefinedIntNoDataValue
        else:
            return int(n)

    def d2udi(self, n):
        if math.isnan(n):
            return self.userDefinedIntNoDataValue
        else:
            return int(n)

class UserDefinedFloatNoDataConversions(object):
    @property
    def userDefinedFloatNoDataValue(self):
        return self._cellType.noDataValue

    # from user defined

    def udf2b(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return BYTEMIN
        else:
            return intToByte(int(n))

    def udf2ub(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return 0
        else:
            return intToByte(int(n))

    def udf2s(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return SHORTMIN
        else:
            return intToShort(int(n))

    def udf2us(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return 0
        else:
            return intToShort(int(n))

    def udf2i(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return INTMIN
        else:
            return int(n)

    def udf2f(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return float("nan")
        else:
            return n

    def udf2d(self, n):
        if n == self.userDefinedFloatNoDataValue:
            return float("nan")
        else:
            return n

    # to user defined

    def b2udf(self, n):
        if n == BYTEMIN:
            return self.userDefinedFloatNoDataValue
        else:
            return float(n)

    def ub2udf(self, n):
        if n == 0:
            return self.userDefinedFloatNoDataValue
        else:
            return float(n)

    def s2udf(self, n):
        if n == SHORTMIN:
            return self.userDefinedFloatNoDataValue
        else:
            return float(n)

    def us2udf(self, n):
        if n == 0:
            return self.userDefinedFloatNoDataValue
        else:
            return float(n)

    def i2udf(self, n):
        if n == INTMIN:
            return self.userDefinedFloatNoDataValue
        else:
            return float(n)

    def f2udf(self, n):
        if math.isnan(n):
            return self.userDefinedFloatNoDataValue
        else:
            return n

    def d2udf(self, n):
        if math.isnan(n):
            return self.userDefinedFloatNoDataValue
        else:
            return n

class UserDefinedDoubleNoDataConversions(object):
    @property
    def userDefinedDoubleNoDataValue(self):
        return self._cellType.noDataValue

    # from user defined

    def udd2b(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return BYTEMIN
        else:
            return intToByte(int(n))

    def udd2ub(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return 0
        else:
            return intToByte(int(n))

    def udd2s(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return SHORTMIN
        else:
            return intToShort(int(n))

    def udd2us(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return 0
        else:
            return intToShort(int(n))

    def udd2i(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return INTMIN
        else:
            return int(n)

    def udd2f(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return float("nan")
        else:
            return n

    def udd2d(self, n):
        if n == self.userDefinedDoubleNoDataValue:
            return float("nan")
        else:
            return n

    # to user defined

    def b2udd(self, n):
        if n == BYTEMIN:
            return self.userDefinedDoubleNoDataValue
        else:
            return float(n)

    def ub2udd(self, n):
        if n == 0:
            return self.userDefinedDoubleNoDataValue
        else:
            return float(n)

    def s2udd(self, n):
        if n == SHORTMIN:
            return self.userDefinedDoubleNoDataValue
        else:
            return float(n)

    def us2udd(self, n):
        if n == 0:
            return self.userDefinedDoubleNoDataValue
        else:
            return float(n)

    def i2udd(self, n):
        if n == INTMIN:
            return self.userDefinedDoubleNoDataValue
        else:
            return float(n)

    def f2udd(self, n):
        if math.isnan(n):
            return self.userDefinedDoubleNoDataValue
        else:
            return n

    def d2udd(self, n):
        if math.isnan(n):
            return self.userDefinedDoubleNoDataValue
        else:
            return n

