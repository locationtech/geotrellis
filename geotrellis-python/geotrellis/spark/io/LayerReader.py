from geotrellis.spark.io.Reader import Reader

class LayerReader(object):
    @property
    def defaultNumPartitions(self):
        pass

    def read(self, K, V, M, _id, *args):
        length = len(args)
        if length == 0:
            return self._read0(K, V, M, _id, *args)
        elif length == 1:
            return self._read1(K, V, M, _id, *args)
        elif length == 2:
            return self._read2(K, V, M, _id, *args)
        elif length == 3:
            return self._read3(K, V, M, _id, *args)

    def _read0(self, K, V, M, _id):
        numPartitions = self.defaultNumPartitions
        return self._read_id_num(K, V, M, _id, numPartitions)

    def _read1(self, K, V, M, _id, numPartitions):
        return self._read_id_num(K, V, M, _id, numPartitions)

    def _read_id_num(self, K, V, M, _id, numPartitions):
        pass

    def reader(self, K, V, M):
        class tempo(Reader):
            def read(innerself, _id):
                return self.read(K, V, M, _id):
        return tempo()
