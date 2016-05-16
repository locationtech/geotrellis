from __future__ import absolute_import

from geotrellis.python.util.CustomComparator import CustomComparatorSeq, CustomComparator
import bisect

def mergeQueue(ranges):
    mq = MergeQueue()
    for r in ranges:
        mq += r
    return mq.toSeq()

class MergeQueue(object):
    def __init__(self, initial_size = 1, lt_func = None):
        self.initial_size = initial_size
        self._lt_func = lt_func
        self._array = []
        self._size = 0

    @property
    def size(self):
        return self._size

    def _remove_element(self, i):
        del self._array[i]
        self._size -= 1

    def _insert_element(self, _range, i):
        self._ensure_size(self._size + 1)
        if i == self._size:
            self._array.append(_range)
        else:
            self._array = self._array[:i] + [_range] + self._array[i:]
        self._size += 1

    def _ensure_size(self, n):
        pass

    def __iadd__(self, _range):
        res = -1
        if self._size != 0:
            lt_func = self._lt_func
            haystack = self._array if lt_func is None else CustomComparatorSeq(self._array, lt_func)
            needle = _range if lt_func is None else CustomComparator(_range, lt_func)
            res = bisect.bisect_left(haystack, needle)
        if res < 0:
            i = -(res + 1)
            this_start, this_end = _range
            remove_left = False
            remove_right = False
            right_remainder = None

            if i != 0:
                prev_start, prev_end = self._array[i-1]
                if prev_start == this_start:
                    remove_left = True
                if prev_end + 1 >= this_start:
                    remove_left = True
                    this_start = prev_start
                    if prev_end > this_end:
                        this_end = prev_end
            if i < self._size and self._size > 0:
                next_start, next_end = self._array[i]
                if this_start == next_start:
                    remove_right = True
                    this_end = next_end
                else:
                    if this_end + 1 >= next_start:
                        remove_right = True
                        if next_end - 1 >= this_end:
                            this_end = next_end
                        elif next_end < this_end - 1:
                            right_remainder = (next_end+1, this_end)
                            this_end = next_end
            if remove_right:
                if not remove_left:
                    self._array[i] = (this_start, this_end)
                else:
                    self._array[i-1] = (this_start, this_end)
                    self._remove_element(i)
            elif remove_left:
                self._array[i-1] = (this_start, this_end)
            else:
                self._insert_element(_range, i)
            if right_remainder is not None:
                self += right_remainder
        return self
    
    @property
    def toSeq(self):
        return self._array[:]
