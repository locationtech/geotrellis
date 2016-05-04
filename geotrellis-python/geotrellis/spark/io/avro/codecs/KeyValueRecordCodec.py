from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from geotrellis.spark.io.avro.codecs.TupleCodec import TupleCodec
from edited_avro.avro_builder import AvroSchemaBuilder

class KeyValueRecordCodec(AvroRecordCodec):
    def __init__(self, key_type, value_type):
        self.pairCodec = TupleCodec(key_type, value_type)

    @property
    def schema(self):
        builder = AvroSchemaBuilder()
        builder.begin_record("KeyValueRecord", namespace = "geotrellis.spark.io")
        builder.add_field("pairs",
                builder.begin_array(self.pairCodec.schema()).end())
        return builder.end()
    
    def _encode(self, pairs, dct):
        dct["pairs"] = map(lambda p: self.pairCodec.encode(p), pairs)

    def decode(self, dct):
        return map(lambda p: self.pairCodec.decode(p), dct["pairs"])
