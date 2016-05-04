from geotrellis.python.util.utils import JSONFormat
from geotrellis.python.spray.json.package_scala import DeserializationException

class LayerHeader(object):
    implicits = {'format': lambda: LayerHeaderFormat()}
    def format(self):
        pass
    def keyClass(self):
        pass
    def valueClass(self):
        pass

class LayerHeaderFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'format', 'keyClass', 'valueClass')
        if not fields:
            raise DeserializationException("LayerHeader expected")
        _format, _key_class, _value_class = fields
        class TempLayerHeader(LayerHeader):
            def format(self):
                return _format
            def keyClass(self):
                return _key_class
            def valueClass(self):
                return _value_class
        return TempLayerHeader()

