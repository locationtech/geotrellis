from __future__ import absolute_import
from geotrellis.proj4.CRS import CRS
#from geotrellis.proj4.LatLng import LatLng
import rasterio.crs

class CRSWrapper(CRS):
    def __init__(self, rasterio_crs, proj4string):
        self.crs = rasterio_crs
        self.proj4string = proj4string

    def __eq__(self, other):
        if not isinstance(other, CRSWrapper):
            return False
        return self.crs == other.crs

    def __hash__(self):
        return hash(tuple(self.crs.items()))

def crsWrapper(proj4string):
    from geotrellis.proj4.LatLng import LatLng
    if proj4string == LatLng.proj4string:
        return LatLng
    else:
        rasterio_crs = rasterio.crs.from_string(proj4string)
        return CRSWrapper(rasterio_crs, proj4string)
