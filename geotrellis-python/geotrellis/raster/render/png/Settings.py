from __future__ import absolute_import

class Settings(object):
    def __init__(self, colorType, filter_):
        self.colorType = colorType
        self.filter = filter_

    def __eq__(self, other):
        if not isinstance(other, Settings):
            return False
        return self.colorType == other.colorType and self.filter == other.filter

    def __hash__(self):
        return hash((self.colorType, self.filter))
