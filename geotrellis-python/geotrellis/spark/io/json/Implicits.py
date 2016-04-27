from geotrellis.python.spray.json.package_scala import DeserializationException
from geotrellis.python.util.utils import JSONFormat

class LayerIdFormat(JSONFormat):
    def from_dict(self, dct):
        from geotrellis.spark.LayerId import LayerId
        fields = self.get_fields(dct, 'name', 'zoom')
        if not fields:
            raise DeserializationException("LayerId expected")
        name, zoom = fields
        return LayerId(name, zoom)

