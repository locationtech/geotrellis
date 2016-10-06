from __future__ import absolute_import
from geotrellis.python.util.utils import JSONFormat
from geotrellis.python.spray.json.package_scala import DeserializationException
import json

class KeyBoundsFormat(JSONFormat):
    def __init__(self, keyformat):
        self.keyformat = keyformat
    def to_dict(self, obj):
        return {"minKey": self.keyformat.to_dict(obj.minKey),
                "maxKey": self.keyformat.to_dict(obj.maxKey)}
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'minKey', 'maxKey')
        if not fields:
            raise DeserializationException(
                    "minKey, maxKey properties expected")
        minkey, maxkey = fields
        from geotrellis.spark.KeyBounds import KeyBounds
        return KeyBounds(
                self.keyformat.from_dict(minkey),
                self.keyformat.from_dict(maxkey))

class SpatialKeyFormat(JSONFormat):
    def to_dict(self, obj):
        return {"col": obj.col,
                "row": obj.row}
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'col', 'row')
        if not fields:
            raise DeserializationException("SpatialKey expected. got: " + json.dumps(dct))
        col, row = fields
        from geotrellis.spark.SpatialKey import SpatialKey
        return SpatialKey(col, row)

class SpaceTimeKeyFormat(JSONFormat):
    def to_dict(self, obj):
        return {"col": obj.col,
                "row": obj.row,
                "instant": obj.instant}
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'col', 'row', 'instant')
        if not fields:
            raise DeserializationException("SpatialKey expected. got: " + json.dumps(dct))
        col, row, instant = fields
        from geotrellis.spark.SpaceTimeKey import SpaceTimeKey
        return SpaceTimeKey(col, row, instant)
