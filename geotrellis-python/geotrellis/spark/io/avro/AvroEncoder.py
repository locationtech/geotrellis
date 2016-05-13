from geotrellis.python.util.utils import find
from avro.io import DatumReader, DatumWriter, BinaryEncoder, BinaryDecoder
import avro.schema as schema
from avro_json_serializer import AvroJsonSerializer
import io
import zlib
import cStringIO
import json

class AvroEncoder(object):

    @staticmethod
    def compress(smth):
        return zlib.compress(smth, 6)

    @staticmethod
    def decompress(smth):
        return zlib.decompress(smth)

    @staticmethod
    def toBinary(thing, codec = None):
        if not codec:
            codec = thing.implicits["AvroRecordCodec"]()
        schema = codec.schema

        writer = DatumWriter(schema)
        bytes_writer = io.BytesIO()
        encoder = BinaryEncoder(bytes_writer)

        writer.write(codec.encode(thing), encoder)

        raw_bytes = bytes_writer.getvalue()
        return AvroEncoder.compress(raw_bytes)

    @staticmethod
    def fromBinary(T, writerSchema, contents = None, codec = None):
        if not codec:
            codec = T.implicits["AvroRecordCodec"]()
        def get_params():
            if contents:
                return writerSchema, contents

            return codec.schema, writerSchema

        writerSchema, contents = get_params()
        schema = codec.schema

        decompressed = AvroEncoder.decompress(contents)
        f = cStringIO.StringIO(decompressed)
        decoder = BinaryDecoder(f)
        datum_reader = DatumReader(writerSchema, schema)
        dct = datum_reader.read(decoder)
        result = codec.decode(dct)
        f.close()
        return result

    @staticmethod
    def toJson(thing, codec = None):
        if not codec:
            codec = thing.implicits["AvroRecordCodec"]()
        schema = codec.schema
        serializer = AvroJsonSerializer(schema)
        return serializer.to_json(codec.encode(thing))

    @staticmethod
    def fromJson(T, jsonString, codec = None):
        if not codec:
            codec = T.implicits["AvroRecordCodec"]()
        schema = codec.schema
        dct = _read_loaded_json(schema, json.loads(jsonString))
        return codec.decode(dct)

# see avro.io.DatumReader._read_default_value
def _read_loaded_json(field_schema, loaded_json):
    if field_schema.type in [ 'null', 'boolean', 'int', 'long', 'float', 'double', 'enum', 'fixed', 'string', 'bytes']:
        return loaded_json
    elif field_schema.type == 'array':
        read_array = []
        for json_val in loaded_json:
            item_val = _read_loaded_json(field_schema.items, json_val)
            read_array.append(item_val)
        return read_array
    elif field_schema.type == 'map':
        read_map = {}
        for key, json_val in loaded_json.items():
            map_val = _read_loaded_json(field_schema.values, json_val)
            read_map[key] = map_val
        return read_map
    elif field_schema.type in ['union', 'error_union']:
        #return _read_loaded_json(field_schema.schemas[0], loaded_json)
        if loaded_json is None:
            nullSchema = find(field_schema.schemas, lambda s:s.type == "null")
            if nullSchema:
                return None
            else:
                fail_msg = "value is None, but no null schema found"
                raise schema.AvroException(fail_msg)
        next_type = loaded_json.keys()[0]
        next_schema = find(field_schema.schemas, lambda s:s.fullname == next_type)
        loaded_json = loaded_json[next_type]
        return _read_loaded_json(next_schema, loaded_json)
    elif field_schema.type == 'record':
        #read_record = {}
        from avro.io import GenericRecord
        read_record = GenericRecord(field_schema)
        for field in field_schema.fields:
            json_val = loaded_json.get(field.name)
            if json_val is None: json_val = field.default
            field_val = _read_loaded_json(field.type, json_val)
            read_record[field.name] = field_val
        return read_record
    else:
        fail_msg = 'Unknown type: %s' % field_schema.type
        raise schema.AvroException(fail_msg)
