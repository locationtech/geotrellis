from __future__ import absolute_import
from geotrellis.spark.KeyBounds import Bounds, EmptyBounds
from geotrellis.python.util.utils import flatten_list

class LayerQuery(object):
    def __init__(self, filterChain = lambda x: x):
        self._filterChain = filterChain

    def __call__(self, metadata):
        #bounds = metadata.getComponent(Bounds[K])
        bounds = metadata.getComponent(Bounds)
        if bounds is EmptyBounds:
            return []
        else:
            keyBounds = bounds
            _, keyBoundsList = self._filterChain((metadata, [keyBounds]))
            return keyBoundsList

    def where(self, exp, layerFilter):
        def newFilterChain(x):
            metadata, keyBoundsList = self._filterChain(x)
            filteredKeyBounds = [layerFilter(metadata, keyBound, exp) for keyBound in keyBoundsList]
            return (metadata, flatten_list(filteredKeyBounds))
        return LayerQuery(newFilterChain)

class BoundLayerQuery(object):
    def __init__(self, query, f):
        self.query = query
        self.f = f

    def where(self, params, layerFilter):
        return BoundLayerQuery(self.query.where(params), f)

    @property
    def result(self):
        return self.f(self.query)
