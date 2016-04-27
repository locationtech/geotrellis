from geotrellis.python.util.utils import JSONFormat
from geotrellis.python.spray.json.package_scala import DeserializationException
import json

class KeyBoundsFormat(JSONFormat):
    def __init__(self, keyformat):
        self.keyformat = keyformat
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
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'col', 'row')
        if not fields:
            raise DeserializationException("SpatialKey expected. got: " + json.dumps(dct))
        col, row = fields
        from geotrellis.spark.SpatialKey import SpatialKey
        return SpatialKey(col, row)
