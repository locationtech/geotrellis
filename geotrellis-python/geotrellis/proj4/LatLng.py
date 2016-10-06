from __future__ import absolute_import
from geotrellis.python.crs.CRSWrapper import CRSWrapper
import rasterio.crs

class _LatLng(CRSWrapper):
    proj4string = "EPSG:4326"
    def __init__(self):
        rasterio_crs = rasterio.crs.from_string(_LatLng.proj4string)
        CRSWrapper.__init__(self, rasterio_crs, self.proj4string)

LatLng = _LatLng()
