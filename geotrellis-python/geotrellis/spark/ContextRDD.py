from pyspark.rdd import RDD
from geotrellis.spark.Metadata import Metadata

class ContextRDD(RDD, Metadata):
    def __init__(self, K, V, rdd, metadata):
        self.K = K
        self.V = V

        jrdd = rdd._jrdd
        ctx = rdd.ctx

        new_jrdd = ctx._jvm.org.apache.spark.api.java.JavaRDD.fromRDD(jrdd)

        RDD.__init__(self, new_jrdd, ctx)

        Metadata.__init__(self, metadata)
        # override val partitioner = rdd.partitioner

    @staticmethod
    def fromTuple(K, V, tup):
        return ContextRDD(K, V, tup[0], tup[1])
