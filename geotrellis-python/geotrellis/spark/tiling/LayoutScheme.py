
class LayoutScheme(object):
    def levelFor(extent, cellSize):
        pass
    def zoomOut(level):
        pass
    def zoomIn(level):
        pass

class LayoutLevel(object):
    def __init__(self, zoom, layout):
        self.zoom = zoom
        self.layout = layout
    def __eq__(self, other):
        if not isinstance(other, LayoutLevel):
            return False
        return self.zoom == other.zoom and self.layout == other.layout
    def __hash__(self):
        return hash((self.zoom, self.layout))
