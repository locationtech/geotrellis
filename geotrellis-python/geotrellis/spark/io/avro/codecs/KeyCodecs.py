from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from edited_avro.avro_builder import AvroSchemaBuilder

from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey

import avro.schema

class SpatialKeyAvroCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, SpatialKey)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("SpatialKey", namespace = "geotrellis.spark")
        _.add_field("col", _.create_int())
        _.add_field("row", _.create_int())

        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, key, dct):
        dct["row"] = key.row
        dct["col"] = key.col

    def decode(self, dct):
        return SpatialKey(dct["col"], dct["row"])

class SpaceTimeKeyAvroCodec(AvroRecordCodec):
    def __init__(self):
        AvroRecordCodec.__init__(self, SpaceTimeKey)

    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("SpatialKey", namespace = "geotrellis.spark")
        _.add_field("col", _.create_int())
        _.add_field("row", _.create_int())
        _.add_field("instant", _.create_long(), aliases = ["millis"])

        dct = _.end()
        return avro.schema.make_avsc_object(dct)

    def _encode(self, key, dct):
        dct["row"] = key.row
        dct["col"] = key.col
        dct["instant"] = key.instant

    def decode(self, dct):
        return SpaceTimeKey(dct["col"], dct["row"], dct["instant"])
