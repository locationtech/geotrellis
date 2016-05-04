from geotrellis.python.util.utils import JSONFormat
from geotrellis.raster.CellType import CellType
from geotrellis.raster.TileLayout import TileLayout

class _CellTypeFormat(JSONFormat):
    def from_dict(self, value):
        if not isinstance(value, str):
            raise DeserializationException("CellType must be a string")
        return CellType.fromString(value)

CellTypeFormat = _CellTypeFormat()

class TileLayoutFormat(JSONFormat):
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
