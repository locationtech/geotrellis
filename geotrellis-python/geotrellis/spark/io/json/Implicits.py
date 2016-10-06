from __future__ import absolute_import
from geotrellis.python.spray.json.package_scala import DeserializationException
from geotrellis.python.util.utils import JSONFormat
from geotrellis.raster.io.json.Implicits import CellTypeFormat
from geotrellis.vector.io.json.GeometryFormats import ExtentFormat
from geotrellis.raster.io.json.Implicits import TileLayoutFormat
from geotrellis.python.crs.CRSWrapper import crsWrapper
from geotrellis.spark.KeyBounds import KeyBounds

class CRSFormat(JSONFormat):
    def to_dict(self, obj):
        return obj.proj4string
    def from_dict(self, string):
        return crsWrapper(string)
    def decode(self, s):
        return self.from_dict(s)

class LayerIdFormat(JSONFormat):
    def to_dict(self, obj):
        return {"name": obj.name,
                "zoom": obj.zoom}
    def from_dict(self, dct):
        from geotrellis.spark.LayerId import LayerId
        fields = self.get_fields(dct, 'name', 'zoom')
        if not fields:
            raise DeserializationException("LayerId expected")
        name, zoom = fields
        return LayerId(name, zoom)

class LayoutDefinitionFormat(JSONFormat):
    def to_dict(self, obj):
        return {"extent": ExtentFormat().to_dict(obj.extent),
                "tileLayout": TileLayoutFormat().to_dict(obj.tileLayout)}
    def from_dict(self, dct):
        fields = self.get_fields(dct, "extent", "tileLayout")
        if not fields:
            raise DeserializationException("LayoutDefinition expected")
        ex, tl = fields
        extent = ExtentFormat().from_dict(ex)
        tileLayout = TileLayoutFormat().from_dict(tl)
        from geotrellis.spark.tiling.LayoutDefinition import LayoutDefinition
        return LayoutDefinition(extent, tileLayout)

class TileLayerMetadataFormat(JSONFormat):
    def __init__(self, K):
        self.K = K

    @property
    def keyBoundsFormat(self):
        K = self.K
        return KeyBounds[K].implicits["format"]()

    def to_dict(self, obj):
        json_ct = CellTypeFormat().to_dict(obj.cellType)
        json_ex = ExtentFormat().to_dict(obj.extent)
        json_ld = LayoutDefinitionFormat().to_dict(obj.layout)
        json_cr = CRSFormat().to_dict(obj.crs)
        json_bn = self.keyBoundsFormat.to_dict(obj.bounds)
        return {"cellType": json_ct,
                "extent": json_ex,
                "layoutDefinition": json_ld,
                "crs": json_cr,
                "bounds": json_bn}

    def from_dict(self, dct):
        fields = self.get_fields(dct, "cellType", "extent", "layoutDefinition", "crs", "bounds")
        if not fields:
            raise DeserializationException("TileLayerMetadata expected")
        ct, ex, ld, cr, bd = fields
        cellType = CellTypeFormat().from_dict(ct)
        layoutDefinition = LayoutDefinitionFormat().from_dict(ld)
        extent = ExtentFormat().from_dict(ex)
        crs = CRSFormat().from_dict(cr)
        bounds = self.keyBoundsFormat.from_dict(bd)
        from geotrellis.spark.TileLayerMetadata import TileLayerMetadata
        return TileLayerMetadata(
                cellType, layoutDefinition, extent, crs, bounds)
