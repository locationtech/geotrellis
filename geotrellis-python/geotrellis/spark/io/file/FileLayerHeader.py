from geotrellis.spark.io.LayerHeader import LayerHeader
from geotrellis.python.util.utils import JSONFormat

class FileLayerHeader(LayerHeader):
    implicits = {'format': lambda: FileLayerHeaderFormat()}
    def __init__(self, key_class, value_class, path):
        self._key_class = key_class
        self._value_class = value_class
        self.path = path
    def format(self):
        return 'file'
    def keyClass(self):
        return self._key_class
    def valueClass(self):
        return self._value_class

class FileLayerHeaderFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'keyClass', 'valueClass', 'path')
        if not fields:
            raise DeserializationException("FileLayerHeader expected")
        key_class, value_class, path = fields
        return FileLayerHeader(key_class, value_class, path)

