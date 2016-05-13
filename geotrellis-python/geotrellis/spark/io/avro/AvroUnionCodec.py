from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from geotrellis.raster.Tile import Tile
from edited_avro.avro_builder import AvroSchemaBuilder
from geotrellis.python.util.utils import fullname
from itertools import ifilter
import avro.schema

class AvroUnionCodec(AvroRecordCodec):
    def __init__(self, record_type, *formats):
        AvroRecordCodec.__init__(self, record_type)
        self.formats = formats

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_union(*[x.schema.to_json() for x in self.formats])
        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def encode(self, thing, dct = None):
        format = self._findFormat(lambda x: x.supported(thing), fullname(type(thing)))
        if dct is None:
            from avro.io import GenericRecord
            dct = GenericRecord(format.schema)
        format._encode(thing, dct)
        return dct

    def _encode(self, tile, dct):
        format = self._findFormat(lambda x: x.supported(thing), fullname(type(thing)))
        format._encode(thing, dct)

    def decode(self, dct):
        #schemaFullName = dct.keys()[0]
        schemaFullName = dct.schema.fullname
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
