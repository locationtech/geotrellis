from __future__ import absolute_import
import math

INTMIN = - (2 ** 31)
INTMAX = - INTMIN - 1
BYTEMIN = - 128
BYTEMAX = 127
SHORTMIN = - (2 ** 15)
SHORTMAX = - SHORTMIN - 1
SHORTRANGE = 2 ** 16

NODATA = INTMIN
byteNODATA = BYTEMIN
ubyteNODATA = 0
shortNODATA = SHORTMIN
ushortNODATA = 0
floatNODATA = float("nan")
doubleNODATA = float("nan")

def isNoData(num):
    if isinstance(num, float):
        return math.isnan(num)
    else:
        return num == NODATA

def isData(num):
    if isinstance(num, float):
        return not math.isnan(num)
    else:
        return num != NODATA

def assertEqualDimensions(first, second):
    if first.dimensions != second.dimensions:
        from geotrellis.raster.RasterExtent import GeoAttrsError
        raise GeoAttrsError("Cannot combine rasters with different dimensions. "
                "{d1} does not match {d2}".format(d1 = first.dimensions, d2 = second.dimensions))

def d2f(num):
    return num # TODO implement in java-compatible way

def f2d(num):
    return num

def d2s(num):
    if math.isnan(num):
        return SHORTMIN
    else:
        return intToShort(int(num))

def d2us(num):
    if math.isnan(num):
        return ushortNODATA
    else:
        return intToShort(int(num))

def d2b(num):
    if math.isnan(num):
        return byteNODATA
    else:
        return intToShort(int(num))

def d2ub(num):
    if math.isnan(num):
        return ubyteNODATA
    else:
        return intToShort(int(num))

def i2b(num):
    if num == INTMIN:
        return BYTEMIN
    else:
        return intToByte(num)

def i2ub(num):
    if num == INTMIN:
        return 0
    else:
        return intToByte(num)

def b2i(num):
    if num == BYTEMIN:
        return INTMIN
    else:
        return num

def b2d(num):
    if num == BYTEMIN:
        return float("nan")
    else:
        return float(num)

def ub2i(num):
    if num == 0:
        return INTMIN
    else:
        return num & 0xFF

def ub2d(num):
    if num == 0:
        return float("nan")
    else:
        return float(num & 0xFF)

def i2s(num):
    if num == INTMIN:
        return SHORTMIN
    else:
        return intToShort(num)

def s2i(num):
    if num == SHORTMIN:
        return INTMIN
    else:
        return num

def s2d(num):
    if num == SHORTMIN:
        return float("nan")
    else:
        return float(num)

def i2us(num):
    if num == INTMIN:
        return 0
    else:
        return intToShort(num)

def us2i(num):
    if num == 0:
        return INTMIN
    else:
        return num & 0xFFFF

def us2d(num):
    if num == 0:
        return float("nan")
    else:
        return float(num & 0xFFFF)

def i2d(num):
    if num == INTMIN:
        return float("nan")
    else:
        return float(num)

def d2i(num):
    if math.isnan(num):
        return INTMIN
    else:
        return int(num)

def i2f(num):
    return i2d(num)

def f2i(num):
    return d2i(num)

def intToByte(n):
    if n > BYTEMAX:
        m = n % 256
        return m if m <= BYTEMAX else m - 256
    elif n < BYTEMIN:
        m = n % (-256)
        return m if m >= BYTEMIN else m + 256
    else:
        return n

def intToShort(n):
    if n > SHORTMAX:
        m = n % SHORTRANGE
        return m if m <= SHORTMAX else m - SHORTRANGE
    elif n < SHORTMIN:
        m = n % (- SHORTRANGE)
        return m if m >= SHORTMIN else m + SHORTRANGE
    else:
        return n
