from geotrellis.python.spray.json.package_scala import DeserializationException
from geotrellis.python.util.utils import JSONFormat
from geotrellis.raster.io.json.Implicits import CellTypeFormat
from geotrellis.vector.io.json.GeometryFormats import ExtentFormat
from geotrellis.raster.io.json.Implicits import TileLayoutFormat
from geotrellis.python.crs.CRSWrapper import CRSWrapper
import rasterio.crs

class CRSFormat(JSONFormat):
    def from_dict(self, dct):
        rasterio_crs = rasterio.crs.from_string(dct)
        return CRSWrapper(rasterio_crs)

class LayerIdFormat(JSONFormat):
    def from_dict(self, dct):
        from geotrellis.spark.LayerId import LayerId
        fields = self.get_fields(dct, 'name', 'zoom')
        if not fields:
            raise DeserializationException("LayerId expected")
        name, zoom = fields
        return LayerId(name, zoom)

class LayoutDefinitionFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, "extent", "tileLayout")
        if not fields:
            raise DeserializationException("LayoutDefinition expected")
        ex, tl = fields
        extent = ExtentFormat().from_dict(ex)
        tileLayout = TileLayoutFormat().from_dict(tl)
        return LayoutDefinition(extent, tileLayout)

class TileLayerMetadataFormat(JSONFormat):
    def __init__(self, K):
        self.K = K

    @property
    def keyBoundsFormat(self):
        K = self.K
        return KeyBounds[K].implicits["format"]()

    def from_dict(self, dct):
        fields = self.get_fields(dct, "cellType", "extent", "layoutDefinition", "crs", "bounds")
        if not fields:
            raise DeserializationException("TileLayerMetadata expected")
        ct, ex, ld, cr, bd = fields
        cellType = CellTypeFormat.from_dict(ct)
        layoutDefinition = LayoutDefinitionFormat().from_dict(ld)
        extent = ExtentFormat().from_dict(ex)
        crs = CRSFormat().from_dict(cr)
        bounds = self.keyBoundsFormat.from_dict(bd)
        return TileLayerMetadata(
                cellType, layoutDefinition, extent, crs, bounds)
