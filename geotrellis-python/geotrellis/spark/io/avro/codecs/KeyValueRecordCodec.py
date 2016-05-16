from __future__ import absolute_import
from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from geotrellis.spark.io.avro.codecs.TupleCodec import TupleCodec
from geotrellis.python.edited_avro.avro_builder import AvroSchemaBuilder
import avro.schema

class KeyValueRecordCodec(AvroRecordCodec):
    def __init__(self, key_type, value_type, keycodec = None, valuecodec = None):
        AvroRecordCodec.__init__(self, list)
        self.pairCodec = TupleCodec(key_type, value_type, keycodec, valuecodec)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("KeyValueRecord", namespace = "geotrellis.spark.io")
        _.add_field("pairs",
                _.begin_array(self.pairCodec.schema.to_json()).end())

        dct = _.end()
        return avro.schema.make_avsc_object(dct)
    
    def _encode(self, pairs, dct):
        dct["pairs"] = map(lambda p: self.pairCodec.encode(p), pairs)

    def decode(self, dct):
        return map(lambda p: self.pairCodec.decode(p), dct["pairs"])
