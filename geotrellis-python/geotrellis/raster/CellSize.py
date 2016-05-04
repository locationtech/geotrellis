import math

class CellSize(object):
    def __init__(self, first, second, third = None):
        width, height = _get_params(first, second, third)
        self.width = width
        self.height = height

    @property
    def resolution(self):
        return math.sqrt(self.width * self.height)

    def __eq__(self, other):
        if not isinstance(other, CellSize):
            return False
        return self.width == other.width and self.height == other.height

    def __hash__(self):
        return hash((self.width, self.height))

    @staticmethod
    def fromString(s):
        width, height = map(lambda word: float(word), s.split(","))
        return CellSize(width, height)

def _get_params(first, second, third):
    if not isinstance(first, Extent):
        return first, second

    extent = first
    if third is None:
        cols, rows = second
    else:
        cols, rows = second, third
    return (extent.width / cols, extent.height / rows)
