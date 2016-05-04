from geotrellis.spark.io.LayerWriter import LayerWriter
from geotrellis.spark.io.file.LayerPath import LayerPath
from geotrellis.spark.io.avro.codecs.KeyValueRecordCodec import KeyValueRecordCodec
from geotrellis.spark.LayerId import LayerId
from geotrellis.spark.io.index.Index import Index
from geotrellis.spark.io.file.KeyPathGenerator import generate_key_path_func
from geotrellis.spark.io.file.FileAttributeStore import FileAttributeStore
from geotrellis.spark.io.package_scala import LayerWriteError

from geotrellis.python.util.utils import fullname
import os.path

class FileLayerWriter(LayerWriter[LayerId]):
    def __init__(self, attributeStore, catalogPath = None):
        attributeStore, catalogPath = _get_params(attributeStore, catalogPath)
        self.attributeStore = attributeStore
        self._catalogPath = catalogPath

    def _write(self, K, V, M, layerid, rdd, keyIndex):
        codec = KeyValueRecordCodec(K, V)
        schema = codec.schema()

        if self.attributeStore.layerExists(layerid):
            raise Exception("{layerid} already exists".format(layerid = str(layerid)))
        path = LayerPath(layerid)
        metadata = rdd.metadata
        header = FileLayerHeader(fullname(K), fullname(V), path)

        maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
        keyPath = generate_key_path_func(self._catalogPath, path, keyIndex, maxWidth)
        layerPath = os.path.join(self._catalogPath, path)

        try:
            self.attributeStore.writeLayerAttributes(layerid, header, metadata, keyIndex, schema)
            FileRDDWriter.write(rdd, layerPath, keyPath)
        except Exception as e:
            raise LayerWriteError(layerid, cause = e)

def _get_params(first, second):
    if second:
        return first, second

    if isinstance(first, FileAttributeStore):
        return first, first.catalogPath
    elif isinstance(first, str):
        return FileAttributeStore(first), first
    else:
        raise Exception("IllegalArguments: {first}, {second}".format(repr(first), repr(second)))
