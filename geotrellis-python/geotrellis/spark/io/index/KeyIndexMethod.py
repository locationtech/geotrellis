import math

class _KeyIndexMethodMeta(object):
    items = {}
    def __getitem__(self, key_type):
        if key_type in self.items.keys():
            return self.items[key_type]
        class tempo(_KeyIndexMethod):
            _key_type = key_type
        self.items[key_type] = tempo
        return tempo

KeyIndexMethod = _KeyIndexMethodMeta()

class _KeyIndexMethod(object):

    _key_type = None

    def resolution(self, _max, _min):
        length = _max - _min + 1
        return int(math.ceil(math.log(length) / math.log(2)))

    def createIndex(self, keyBounds):
        pass
