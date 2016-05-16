from __future__ import absolute_import
from geotrellis.spark.io.json.Implicits import LayerIdFormat

class LayerId(object):
    implicits = {'format': lambda: LayerIdFormat()}
    def __init__(self, name, zoom):
        self.name = name
        self.zoom = zoom

    def __eq__(self, other):
        if not isinstance(other, LayerId):
            return False
        return self.name == other.name and self.zoom == other.zoom

    def __hash__(self):
        return hash((self.name, self.zoom))

    def __str__(self):
        return "Layer(name=\"{0}\", zoom={1})".format(self.name, self.zoom)

    @staticmethod
    def fromTuple(tup):
        return LayerId(tup[0], tup[1])

