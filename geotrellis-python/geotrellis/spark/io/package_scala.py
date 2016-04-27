

class LayerIOError(Exception):
    def __init__(self, msg):
        self.msg = msg

class TileNotFoundError(LayerIOError):
    def __init__(self, key, layer_id):
        msg = ("Tile with key " + str(key) + 
            " not found for layer " + str(layer_id))
        LayerIOError.__init__(self, msg)

class AttributeNotFoundError(LayerIOError):
    def __init__(self, attr_name, layer_id):
        msg = ("Attribute " + attr_name + 
            " not found for layer " + str(layer_id))
        LayerIOError.__init__(self, msg)

class LayerNotFoundError(LayerIOError):
    def __init__(self, layer_id):
        msg = ("Layer " + str(layer_id) +
                " not found in the catalog")
        LayerIOError.__init__(self, msg)

