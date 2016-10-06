from __future__ import absolute_import
class Reader(object):
    def read(self, key):
        pass
    def __call__(self, key):
        return self.read(key)
