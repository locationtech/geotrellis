from __future__ import absolute_import
from geotrellis.spark.io.avro.codecs.KeyValueRecordCodec import KeyValueRecordCodec
from geotrellis.util.Filesystem import Filesystem
from geotrellis.spark.io.avro.AvroEncoder import AvroEncoder

class FileRDDWriter(object):

    @staticmethod
    def write(K, V, rdd, rootPath, keyPath):
        codec = KeyValueRecordCodec(K, V)
        schema = codec.schema

        pathsToFiles = rdd.groupBy(
                lambda row: keyPath(row[0]),
                numPartitions = rdd.getNumPartitions())
        Filesystem.ensureDirectory(rootPath)

        def func(t):
            path, rows = t
            bytesArray = AvroEncoder.toBinary(rows, codec)
            Filesystem.writeBytes(path, bytesArray)

        pathsToFiles.foreach(func)
