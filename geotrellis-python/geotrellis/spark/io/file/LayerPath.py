import os.path

class _LayerPath(object):
    def __call__(self,  first, second = None):
        if second is None:
            layerid = first
            return _namezoom(layerid)
        else:
            catalogPath = first
            layerid = second
            return os.path.join(catalogPath, _namezoom(layerid))

def _namezoom(layerid):
    return "{name}/{zoom}".format(name = layerid.name, zoom = layerid.zoom)

LayerPath = _LayerPath()
