from geotrellis.python.spray.json.package_scala import DeserializationException
from geotrellis.python.util.utils import JSONFormat
from geotrellis.vector.Extent import Extent

class ExtentFormat(JSONFormat):
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
