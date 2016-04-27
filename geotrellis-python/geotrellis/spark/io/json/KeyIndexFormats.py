from geotrellis.python.util.utils import JSONFormat
from geotrellis.python.spray.json.package_scala import DeserializationException
from geotrellis.spark.io.json.KeyFormats import KeyBoundsFormat, SpatialKeyFormat

class ZSpatialKeyIndexFormat(JSONFormat):
    TYPE_NAME = "zorder"

    def from_dict(self, dct):
        fields = self.get_fields(dct, 'type', 'properties')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex type:" +
                    " ZSpatialKeyIndex expected.")
        typename, props = fields
        if typename != ZSpatialKeyIndexFormat.TYPE_NAME:
            raise DeserializationException(
                    "Wrong KeyIndex type: {0} expected.".format(
                        ZSpatialKeyIndexFormat.TYPE_NAME))
        fields = self.get_fields(props, 'keyBounds')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex constructor arguments:" +
                    " ZSpatialKeyIndex constructor arguments expected.")
        key_bounds_dict = fields[0]
        key_bounds = KeyBoundsFormat(SpatialKeyFormat()).from_dict(key_bounds_dict)
        from geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex import ZSpatialKeyIndex
        return ZSpatialKeyIndex(key_bounds)

