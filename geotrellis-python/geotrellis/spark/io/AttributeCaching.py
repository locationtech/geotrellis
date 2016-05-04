class AttributeCaching(object):
    def __init__(self):
        self._cache = {}

    def cacheRead(self, attr_type, layer_id, attr_name):
        tup = (layer_id, attr_name)
        if tup in self._cache:
            return self._cache[tup]
        else:
            attr = self.read(attr_type, layer_id, attr_name)
            self._cache[tup] = attr
            return attr

    def cacheWrite(self, attr_type, layer_id, attr_name, value):
        tup = (layer_id, attr_name)
        self._cache[tup] = value
        self.write(attr_type, layer_id, attr_name, value)

    def clearCache(self, layer_id = None, attr_name = None):
        if layer_id is None:
            if attr_name is None:
                self._cache.clear()
            else:
                raise Exception("cannot clear attr when layer_id is unknown")
        else:
            if attr_name is None:
                for tup in filter(lambda each: each[0] == layer_id, self._cache):
                    del self._cache[tup]
            else:
                del self._cache[(layer_id, attr_name)]

