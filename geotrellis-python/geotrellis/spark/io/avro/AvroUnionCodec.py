from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from geotrellis.raster.Tile import Tile
from edited_avro.avro_builder import AvroSchemaBuilder
from geotrellis.python.util.utils import fullname
from itertools import ifilter

class AvroUnionCodec(AvroRecordCodec):
    def __init__(self, record_type, *formats):
        AvroRecordCodec.__init__(self, record_type)
        self.formats = formats

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_union(*[x.schema for x in self.formats])
        return _.end()

    def encode(self, thing, dct = None):
        if dct is None:
            dct = {}
        format = self._findFormat(lambda x: x.supported(thing), fullname(type(thing)))
        format._encode(thing, dct)
        return dct

    def _encode(self, tile, dct):
        format = self._findFormat(lambda x: x.supported(thing), fullname(type(thing)))
        format._encode(thing, dct)

    def decode(self, dct):
        schemaFullName = dct.keys().head
        format = self._findFormat(
                lambda x: x.schema.fullname == schemaFullName,
                schemaFullName)
        return format.decode(dct)

    def _findFormat(self, predicate, target):
        filtered = [f for f in ifilter(predicate, self.formats)]
        length = len(filtered)
        if length == 1:
            return filtered[0]
        elif length == 0:
            raise Exception(
                    "No formats found to support {target}".format(
                        target = target))
        else:
            raise Exception(
                    "Multiple formats support {target}: {formats}".format(
                        target = target,
                        formats = filtered))
