
class _KeyIndexMeta(object):
    items = {}
    def __getitem__(self, key):
        if key in self.items:
            return self.items[key]
        class tempo(_KeyIndex):
            pass
        self.items[key] = tempo
        return tempo

KeyIndex = _KeyIndexMeta()

class _KeyIndex(object):
    @property
    def keyBounds(self):
        pass
    def toIndex(self, key):
        pass
    def indexRanges(self, keyRange):
        pass

