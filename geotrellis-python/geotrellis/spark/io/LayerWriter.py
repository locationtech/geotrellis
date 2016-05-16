from __future__ import absolute_import
from geotrellis.spark.io.Writer import Writer
from geotrellis.spark.io.index.KeyIndex import KeyIndex
from geotrellis.spark.io.index.KeyIndexMethod import KeyIndexMethod
from geotrellis.spark.KeyBounds import Bounds, EmptyBounds

class _LayerWriterMeta(object):
    items = {}
    def __getitem__(self, type_param):
        if type_param in self.items.keys():
            return self.items[type_param]
        class tempo(_LayerWriter):
            id_type = type_param
        self.items[type_param] = tempo
        return tempo


LayerWriter = _LayerWriterMeta()

class _LayerWriter(object):
    def _write(self, K, V, M, _id, layer, keyIndex):
        pass
    
    def write(self, K, V, M, _id, layer, keyIndexOrMethod):
        #bounds = layer.metadata.getComponent(Bounds[K])
        bounds = layer.metadata.getComponent(Bounds)
        if bounds is EmptyBounds:
            raise EmptyBoundsError("Cannot write layer with empty bounds.")
        def getKeyIndex():
            if isinstance(keyIndexOrMethod, KeyIndex[K]):
                return keyIndexOrMethod
            elif isinstance(keyIndexOrMethod, KeyIndexMethod[K]):
                keyIndexMethod = keyIndexOrMethod
                return keyIndexMethod.createIndex(bounds)
            else:
                raise Exception("wrong keyIndex or keyIndexMethod type")
        keyIndex = getKeyIndex()
        self._write(K, V, M, _id, layer, keyIndex)

    def writer(self, K, V, M, keyIndexOrMethod):
        class tempo(Writer): 
            def write(innerself, _id, layer):
                self.write(K, V, M, _id, layer, keyIndexOrMethod)
        return tempo()
