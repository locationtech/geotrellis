from avro.io import DatumReader, DatumWriter, BinaryEncoder, BinaryDecoder
import io
import zlib
import cStringIO

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

            contents = writerSchema
            return codec.schema, contents

        writerSchema, contents = get_params()
        schema = codec.schema

        decompressed = AvroEncoder.decompress(contents)
        with cStringIO.StringIO(decompressed) as f:
            decoder = BinaryDecoder(f)
            datum_reader = DatumReader(writerSchema, schema)
            dct = datum_reader.read(decoder)
            return codec.decode(dct)
