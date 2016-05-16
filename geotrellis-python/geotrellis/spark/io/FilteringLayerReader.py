from __future__ import absolute_import
from geotrellis.spark.io.LayerQuery import LayerQuery, BoundLayerQuery
from geotrellis.spark.io.LayerReader import LayerReader

class FilteringLayerReader(LayerReader):

    def _read1(self, K, V, M, _id, rasterQuery):
        return self._read2(K, V, M, _id, rasterQuery, self.defaultNumPartitions)

    def _read2(self, K, V, M, _id, rasterQuery, numPartitions):
        indexFilterOnly = False
        return self._read3(K, V, M, _id, rasterQuery, numPartitions, indexFilterOnly)

    def _read3(self, K, V, M, _id, rasterQuery, numPartitions, indexFilterOnly):
        pass

    def _read_id_num(self, K, V, M, _id, numPartitions):
        return self._read2(K, V, M, _id, LayerQuery(), numPartitions)

    def query(self, K, V, M, layerid, numPartitions = None):
        if numPartitions is None:
            return BoundLayerQuery(LayerQuery(), lambda x: self.read(K, V, M, layerid, x))
        else:
            return BoundLayerQuery(LayerQuery(), lambda x: self.read(K, V, M, layerid, x, numPartitions))

