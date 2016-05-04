class CRS(object):
    pass

class CRSWrapper(CRS):
    def __init__(self, rasterio_crs):
        self.crs = rasterio_crs

    def __eq__(self, other):
        if not isinstance(other, CRSWrapper):
            return False
        return self.crs == other.crs

    def __hash__(self):
        return hash(tuple(self.crs.items()))
