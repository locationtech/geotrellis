from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from edited_avro.avro_builder import AvroSchemaBuilder

class TupleCodec(AvroRecordCodec):
    def __init__(self, keytype, valuetype):
        AvroRecordCodec.__init__(self, tuple)
        self.keytype = keytype
        self.valuetype = valuetype
        self.a = keytype.implicits["AvroRecordCodec"]()
        self.b = valuetype.implicits["AvroRecordCodec"]()

    @property
    def schema(self):
        builder = AvroSchemaBuilder()
        builder.begin_record("Tuple2", namespace = "scala")
        builder.add_field("_1",
                builder.begin_with_schema_json(self.a.schema).end())
        builder.add_field("_2",
                builder.begin_with_schema_json(self.b.schema).end())
        return builder.end()

    def _encode(self, tup, dct):
        dct["_1"] = self.a.encode(tup[0])
        dct["_2"] = self.b.encode(tup[1])

    def decode(self, dct):
        return (self.a.decode(dct["_1"]),
                self.b.decode(dct["_2"]))
