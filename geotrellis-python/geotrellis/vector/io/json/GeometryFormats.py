from __future__ import absolute_import
from geotrellis.python.spray.json.package_scala import DeserializationException
from geotrellis.python.util.utils import JSONFormat
from geotrellis.vector.Extent import Extent

class ExtentFormat(JSONFormat):
    def to_dict(self, obj):
        return {"xmin": obj.xmin,
                "ymin": obj.ymin,
                "xmax": obj.xmax,
                "ymax": obj.ymax }

    def from_dict(self, dct):
        fields = self.get_fields(dct, "xmin", "ymin", "xmax", "ymax")
        if not fields:
            raise DeserializationException(
                    "Extent [xmin,ymin,xmax,ymax] expected")
        try:
            xmi, ymi, xma, yma = fields
            xmin, ymin, xmax, ymax = float(xmi), float(ymi), float(xma), float(yma)
            return Extent(xmin, ymin, xmax, ymax)
        except ValueError:
            raise DeserializationException(
                    "Extent [xmin,ymin,xmax,ymax] expected")
