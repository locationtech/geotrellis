from geotrellis.spark.io.FilteringLayerReader import FilteringLayerReader
from geotrellis.spark.io.package_scala import LayerNotFoundError
from geotrellis.spark.io.index.Index import Index
from geotrellis.spark.io.file.KeyPathGenerator import generate_key_path_func
from geotrellis.spark.io.file.FileRDDReader import FileRDDReader
from geotrellis.spark.io.file.FileAttributeStore import FileAttributeStore
from geotrellis.spark.ContextRDD import ContextRDD

def _get_params(first, second):
    if second is None:
        if isinstance(first, FileAttributeStore):
            return first, first.catalogPath
        elif isinstance(first, str):
            return FileAttributeStore(first), first
        else:
            raise Exception("wrong params ({0}, {1})".format(str(first), str(second)))
    else:
        return first, second

class FileLayerReader(FilteringLayerReader):
    def __init__(self, sc, attributeStore, catalogPath = None):
        attribute_store, catalog_path = _get_params(attributeStore, catalogPath)
        self.attributeStore = attributeStore
        self._catalogPath = catalogPath
        self._sc = sc

    @property
    def defaultNumPartitions(self):
        return self._sc.defaultParallelism

    def _read3(self, K, V, M, _id, rasterQuery, numPartitions, filterIndexOnly):
        if not self.attributeStore.layerExists(_id):
            raise LayerNotFoundError(_id)
        attrs = None
        try:
            attrs = self.attributeStore.readLayerAttributes(FileLayerHeader, M, K, _id)
        except AttributeNotFoundError as e:
            raise LayerReadError(_id, cause = e)
        header = attrs.header
        metadata = attrs.metadata
        keyIndex = attrs.keyIndex
        writerSchema = attrs.schema

        layerPath = header.path
        queryKeyBounds = rasterQuery(metadata)
        maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
        keyPath = generate_key_path_func(catalogPath, layerPath, maxWidth)
        decompose = lambda bounds: keyIndex.indexRanges(bounds)
        rdd = FileRDDReader.read(K, V, keyPath, queryKeyBounds, decompose, filterIndexOnly, writerSchema, numPartitions)
        return ContextRDD(K, V, rdd, metadata)
