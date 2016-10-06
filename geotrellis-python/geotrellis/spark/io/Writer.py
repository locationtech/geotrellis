from __future__ import absolute_import
class Writer(object):
    def write(self, key, value):
        pass
    def __call__(self, key, value):
        self.write(key, value)
