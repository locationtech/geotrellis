
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
    def key_bounds(self):
        pass
    def to_index(self, key):
        pass
    def index_ranges(self, key_range):
        pass

