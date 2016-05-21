from __future__ import absolute_import

class Filter(object):
    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return False
        return self.n == other.n
    def __hash__(self):
        return hash((self.n,))

def _filter(n_):
    class tempo(Filter):
        n = n_
    return tempo

class _NoFilter(_filter(0)): pass
NoFilter = _NoFilter()

class _SubFilter(_filter(1)): pass
SubFilter = _SubFilter()

class _UpFilter(_filter(2)): pass
UpFilter = _UpFilter()

class _AvgFilter(_filter(3)): pass
AvgFilter = _AvgFilter()

class _PaethFilter(_filter(4)): pass
PaethFilter = _PaethFilter()

