from __future__ import absolute_import
from geotrellis.spark.io.index.MergeQueue import mergeQueue
from geotrellis.spark.io.index.IndexRanges import IndexRanges
from geotrellis.spark.io.avro.AvroEncoder import AvroEncoder
from geotrellis.spark.io.avro.codecs.KeyValueRecordCodec import KeyValueRecordCodec
from geotrellis.spark.KeyBounds import KeyBounds
from geotrellis.python.util.utils import getOrElse, file_exists, flat_map

class FileRDDReader(object):
    def __init__(self, sc):
        self.sc = sc

    def read(self,
            K, V,
            keyPath,
            queryKeyBounds,
            decomposeBounds,
            filterIndexOnly,
            writerSchema = None,
            numPartitions = None):
        if not queryKeyBounds:
            return self.sc.emptyRDD()

        ranges = flat_map(queryKeyBounds, decomposeBounds)
        if len(queryKeyBounds) > 1:
            ranges = mergeQueue(ranges)
        bins = IndexRanges.bin(ranges, getOrElse(numPartitions, self.sc.defaultParallelism))
        boundable = K.implicits["Boundable"]() # TODO will it work for all types?
        includeKey = lambda key: KeyBounds.includeKey(queryKeyBounds, key, boundable)
        recordCodec = KeyValueRecordCodec(K, V)
        # kwWriterSchema = KryoWrapper(writerSchema)

        def mapper(partition):
            resultPartition = [[]] # we have to wrap it to be able to use it in inner function

            def append_from(path):
                if not file_exists(path):
                    return
                with open(path) as f:
                    bytesarray = f.read()
                    recs = AvroEncoder.fromBinary(
                            getOrElse(writerSchema, recordCodec.schema),
                            bytesarray,
                            codec = recordCodec)
                    if filterIndexOnly:
                        resultPartition[0] += recs
                    else:
                        resultPartition[0] += filter(lambda row: includeKey(row[0]), recs)

            for rangeList in partition:
                for _range in rangeList:
                    start, end = _range
                    for index in xrange(start, end+1):
                        path = keyPath(index)
                        append_from(path)
            return resultPartition[0]

        return self.sc.parallelize(bins, len(bins)).mapPartitions(mapper)
