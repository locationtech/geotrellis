from __future__ import absolute_import

class ExceptionWithCause(Exception):
    def __init__(self, msg, **kwargs):
        self.msg = msg
        if "cause" in kwargs.keys():
            self.cause = kwargs["cause"]
            self.msg = self.msg + ", caused by " + repr(self.cause)
        Exception.__init__(self, self.msg)

class LayerIOError(ExceptionWithCause):
    def __init__(self, msg, **kwargs):
        ExceptionWithCause.__init__(self, msg, **kwargs)

class TileNotFoundError(LayerIOError):
    def __init__(self, key, layer_id, **kwargs):
        msg = ("Tile with key " + str(key) + 
            " not found for layer " + str(layer_id))
        LayerIOError.__init__(self, msg, **kwargs)

class AttributeNotFoundError(LayerIOError):
    def __init__(self, attr_name, layer_id, **kwargs):
        msg = ("Attribute " + attr_name + 
            " not found for layer " + str(layer_id))
        LayerIOError.__init__(self, msg, **kwargs)

class LayerNotFoundError(LayerIOError):
    def __init__(self, layer_id, **kwargs):
        msg = ("Layer " + str(layer_id) +
                " not found in the catalog")
        LayerIOError.__init__(self, msg, **kwargs)

class LayerWriteError(LayerIOError):
    def __init__(self, layer_id, message = "", **kwargs):
        msg = ("Failed to write {layerid}".format(layerid = str(layer_id)) +
                message if not message else ": " + message)
        LayerIOError.__init__(self, msg, **kwargs)

