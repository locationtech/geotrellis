from __future__ import absolute_import
from geotrellis.util.GetComponent import GetComponent
from geotrellis.util.SetComponent import SetComponent

class Component(GetComponent, SetComponent):
    pass

def component(_get, _set):
    class TempComponent(Component):
        def get(self):
            return _get
        def set(self):
            return _set
    return TempComponent()

