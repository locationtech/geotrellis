from geotrellis.python.util.utils import fold_left

class IndexRanges(object):

    @staticmethod
    def bin(ranges, count):
        stack = list(ranges)
        def leng(r):
            return r[1] - r[0] + 1
        total = fold_left(ranges, 0, lambda s, r: s + leng(r))
        binWidth = total / count + 1

        def splitRange(_range, take):
            if leng(_range) <= take:
                raise Exception("Assertion failed")
            first = (_range[0], _range[0] + take - 1)
            second = (_range[0] + take, _range[1])
            return (first, second)

        arr = [[]] * count
        summ = 0
        i = 0
        while stack:
            head = stack[0]
            if leng(head) + summ <= binWidth:
                arr[i].insert(0, head)
                summ += leng(head)
                stack = stack[1:]
            else:
                take, left = splitRange(head, binWidth - summ)
                stack[0] = left
                arr[i].insert(0, take)
                summ += leng(take)
            if summ >= binWidth:
                sum = 0
                i += 1
        return arr
            


