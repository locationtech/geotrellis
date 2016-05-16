from __future__ import absolute_import
from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from geotrellis.python.edited_avro.avro_builder import AvroSchemaBuilder
import avro.schema

class TupleCodec(AvroRecordCodec):
    def __init__(self, keytype, valuetype, keycodec = None, valuecodec = None):
        AvroRecordCodec.__init__(self, tuple)
        self.keytype = keytype
        self.valuetype = valuetype

        if keycodec is None:
            self.a = keytype.implicits["AvroRecordCodec"]()
        else:
            self.a = keycodec

        if valuecodec is None:
            self.b = valuetype.implicits["AvroRecordCodec"]()
        else:
            self.b = valuecodec

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("Tuple2", namespace = "scala")
        _.add_field("_1", self.a.schema.to_json())
        _.add_field("_2", self.b.schema.to_json())

        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, tup, dct):
        dct["_1"] = self.a.encode(tup[0])
        dct["_2"] = self.b.encode(tup[1])

    def decode(self, dct):
        return (self.a.decode(dct["_1"]),
                self.b.decode(dct["_2"]))
