from geotrellis.spark.io.json.Implicits import LayerIdFormat

class LayerId(object):
    implicits = {'format': lambda: LayerIdFormat()}
    def __init__(self, name, zoom):
        self.name = name
        self.zoom = zoom

    def __str__(self):
        return "Layer(name=\"{0}\", zoom={1})".format(self.name, self.zoom)

    @staticmethod
    def from_tuple(tup):
        return LayerId(tup[0], tup[1])

