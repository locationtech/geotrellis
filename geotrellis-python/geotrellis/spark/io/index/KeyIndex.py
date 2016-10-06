from __future__ import absolute_import

def _formatGeneratorFunc(key):
    def func():
        from geotrellis.spark.io.json.KeyIndexFormats import KeyIndexJsonFormatFactory
        return KeyIndexJsonFormatFactory.getKeyIndexJsonFormat(key)
    return func

class _KeyIndexMeta(object):
    items = {}
    def __getitem__(self, key):
        if key in self.items:
            return self.items[key]
        class tempo(_KeyIndex):
            implicits = {"format": _formatGeneratorFunc(key)}
        self.items[key] = tempo
        return tempo

KeyIndex = _KeyIndexMeta()

class _KeyIndex(object):
    @property
    def keyBounds(self):
        return self._keyBounds
    def toIndex(self, key):
        pass
    def indexRanges(self, keyRange):
        pass

