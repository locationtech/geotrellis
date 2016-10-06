from __future__ import absolute_import
from geotrellis.python.util.utils import JSONFormat
from geotrellis.python.spray.json.package_scala import DeserializationException

class LayerHeader(object):
    implicits = {'format': lambda: LayerHeaderFormat()}
    @property
    def format(self):
        pass
    @property
    def keyClass(self):
        pass
    @property
    def valueClass(self):
        pass

class LayerHeaderFormat(JSONFormat):
    def to_dict(self, obj):
        return {"format": obj.format,
                "keyClass": obj.keyClass,
                "valueClass": obj.valueClass}
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'format', 'keyClass', 'valueClass')
        if not fields:
            raise DeserializationException("LayerHeader expected")
        _format, _key_class, _value_class = fields
        class TempLayerHeader(LayerHeader):
            @property
            def format(self):
                return _format
            @property
            def keyClass(self):
                return _key_class
            @property
            def valueClass(self):
                return _value_class
        return TempLayerHeader()

