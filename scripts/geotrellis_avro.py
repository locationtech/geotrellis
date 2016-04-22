from avro.datafile import DataFileReader
from avro.io import DatumReader, BinaryDecoder
import avro.schema 

import zlib
import json
import tempfile
import numpy

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

def to_pairs(dct):
    return [Pair.from_dict(rec) for rec in dct['pairs']]

class Pair():
    def __init__(self, first, second):
        self._1 = first
        self._2 = second

    def is_suitable_dict(dct):
        return '_1' in dct

    @staticmethod
    def from_dict(dct):
        pos = LayoutPosition.from_dict(dct['_1'])
        bands = BandsContainer.from_dict(dct['_2'])
        return Pair(pos, bands)

class LayoutPosition():
    def __init__(self, col, row):
        self.col = col
        self.row = row

    def is_suitable_dict(dct):
        return 'col' in dct

    @staticmethod
    def from_dict(dct):
        return LayoutPosition(dct['col'], dct['row'])

class BandsContainer():
    def __init__(self, bands):
        self.bands = bands

    def is_suitable_dict(dct):
        return 'bands' in dct

    @staticmethod
    def from_dict(dct):
        bands = [Band.from_dict(rec) for rec in dct['bands']]
        return BandsContainer(bands)

class Band():
    def __init__(self, cells, cols, rows, no_data_value):
        self.cells = numpy.array(cells).reshape(rows, cols)
        self.cols = cols
        self.rows = rows
        self.no_data_value = no_data_value

    def is_suitable_dict(dct):
        return 'cells' in dct

    @staticmethod
    def from_dict(dct):
        return Band(dct['cells'], dct['cols'], dct['rows'], dct['noDataValue'])


def doit(path_to_landsat):
    cat = path_to_landsat + '/data/catalog'
    schema = fetch_schema_string(cat + '/attributes/landsat__.__0__.__metadata.json')
    return to_pairs(from_file(schema, cat + '/landsat/0/0'))
