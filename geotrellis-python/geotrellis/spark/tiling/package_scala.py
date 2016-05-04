from geotrellis.vector.Extent import Extent
from geotrellis.vector.reproject.Implicits import reproject

_WORLD_WSG84 = Extent(-180, -89.99999, 179.99999, 89.99999)

def worldExtent(crs):
    if _isLatLng(crs):
        return _WORLD_WSG84
    elif _isWebMercator(crs):
        return Extent(-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244)
    else:
        return reproject(LatLng, _WORLD_WSG84, crs)

# TODO

LatLng = None
def _isLanLng(crs):
    pass
def _isWebMercator(crs):
    pass
