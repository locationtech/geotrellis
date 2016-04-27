
class CustomComparator(object):
    def __init__(self, value, lt_func):
        self.value = value
        self.lt_func = lt_func
    def __lt__(self, other):
        return self.lt_func(self.value, other)

class CustomComparatorSeq(object):
    def __init__(self, seq, lt_func):
        self.seq = seq
        self.lt_func = lt_func
    def __getitem__(self, key):
        return CustomComparator(self.seq[key], lt_func)
    def __len__(self):
        return len(self.seq)
