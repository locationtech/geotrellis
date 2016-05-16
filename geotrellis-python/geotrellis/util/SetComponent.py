from __future__ import absolute_import
class SetComponent(object):
    def set(self):
        pass

def set_component(_set):
    class TempSetComponent(SetComponent):
        def set(self):
            return _set
    return TempSetComponent()

