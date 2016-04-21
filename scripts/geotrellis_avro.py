from avro.datafile import DataFileReader
from avro.io import DatumReader, BinaryDecoder
import avro.schema 

import zlib
import json
import tempfile

def fetch_schema_string(file_path):
    attrs = read_attributes(file_path)
    return json.dumps(attrs[1]['schema'])

def read_attributes(file_path):
    with open(file_path, 'r') as f:
        return json.loads(f.read())

def from_file(schema_json_string, file_path):
    schema = avro.schema.parse(schema_json_string)
    handle, tmp_path = tempfile.mkstemp()
    with open(file_path, "rb") as file_compressed:
        decompressed = zlib.decompress(file_compressed.read())
        with tempfile.TemporaryFile() as f:
            f.write(decompressed)
            f.seek(0)
            decoder = BinaryDecoder(f)
            datum_reader = DatumReader(schema)
            return datum_reader.read(decoder)

def from_avro_file(file_path):
    with open(file_path, "rb") as f:
        with DataFileReader(f, DatumReader()) as reader:
            result = [rec for rec in reader]
            return result

def doit(path_to_landsat):
    cat = path_to_landsat + '/data/catalog'
    schema = fetch_schema_string(cat + '/attributes/landsat__.__0__.__metadata.json')
    return from_file(schema, cat + '/landsat/0/0')
