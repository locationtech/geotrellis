from __future__ import absolute_import
from geotrellis.python.util.utils import JSONFormat
from geotrellis.raster.CellType import CellType
from geotrellis.raster.TileLayout import TileLayout
from geotrellis.python.spray.json.package_scala import DeserializationException

class CellTypeFormat(JSONFormat):
    def to_dict(self, obj):
        return str(obj)

    def from_dict(self, value):
        if not isinstance(value, str) and not isinstance(value, unicode):
            raise DeserializationException("CellType must be a string")
        return CellType.fromString(value)

    def decode(self, s):
        return self.from_dict(s)

class TileLayoutFormat(JSONFormat):
    def to_dict(self, obj):
        return {"layoutCols":   obj.layoutCols,
                "layoutRows":   obj.layoutRows,
                "tileCols":     obj.tileCols,
                "tileRows":     obj.tileRows }
    def from_dict(self, dct):
        fields = self.get_fields(dct, "layoutCols", "layoutRows", "tileCols", "tileRows")
        if not fields:
            raise DeserializationException("TileLayout expected")
        try:
            lc, lr, tc, tr = fields
            layoutCols, layoutRows = int(lc), int(lr)
            tileCols, tileRows = int(tc), int(tr)
            return TileLayout(layoutCols, layoutRows, tileCols, tileRows)
        except ValueError:
            raise DeserializationException("TileLayout expected")
