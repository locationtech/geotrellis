from __future__ import absolute_import
from pyspark.rdd import RDD
from geotrellis.spark.Metadata import Metadata

class ContextRDD(RDD, Metadata):
    def __init__(self, rdd, metadata):
        jrdd = rdd._jrdd
        ctx = rdd.ctx

        #new_jrdd = ctx._jvm.JavaRDD.fromRDD(jrdd.rdd(), None)
        new_jrdd = jrdd

        RDD.__init__(self, new_jrdd, ctx)

        Metadata.__init__(self, metadata)
        # override val partitioner = rdd.partitioner

    @staticmethod
    def fromTuple(tup):
        return ContextRDD(tup[0], tup[1])
