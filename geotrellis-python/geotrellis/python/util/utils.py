from __future__ import absolute_import
from __future__ import absolute_import
import os.path

_INTMIN = - (2 ** 31)
_INTMAX = - _INTMIN - 1

def isValidInt(num):
    return _INTMIN <= num <= _INTMAX

import math

def float_eq(a, b):
    if math.isnan(a):
        return math.isnan(b)
    elif math.isnan(b):
        return False
    elif math.isinf(a):
        return math.isinf(b) and a * b > 0
    elif math.isinf(b):
        return False
    else:
        return isclose(a, b)

# floating-point numbers comparator
def isclose(a, b, rel_tol=1e-06, abs_tol=0.0):
    return abs(a-b) <= max(rel_tol * max(abs(a), abs(b)), abs_tol)

def file_exists(path):
    return os.path.isfile(path)

def dir_exists(path):
    return os.path.isdir(path)

def fullname(typeObject):
    name = typeObject.__name__
    module = typeObject.__module__
    lastdot = module.rfind(".")
    withoutLast = module[:lastdot]
    return withoutLast + "." + name

def getOrElse(first, second):
    return first if first is not None else second

from json import JSONEncoder, JSONDecoder

class JSONFormat(object):
    def get_fields(self, dct, *fieldnames):
        try:
            return map(lambda name: dct[name], fieldnames)
        except KeyError:
            return None

    def encode(self, o):
        return self.to_dict(o)

    def decode(self, s):
        dct = JSONDecoder.decode(self, s)
        return self.from_dict(dct)

    @property
    def encoder(self):
        class tempo(JSONEncoder):
            def encode(innerself, o):
                return self.encode(o)
        return tempo

    @property
    def decoder(self):
        class tempo(JSONDecoder):
            def decode(innerself, dct):
                return self.decode(dct)
        return tempo

import avro.schema 

class SchemaFormat(JSONFormat):
    def to_dict(self, o):
        return o.to_json()
    def from_dict(self, dct):
        return avro.schema.make_avsc_object(dct)
    def decode(self, s):
        return avro.schema.parse(s)

from itertools import ifilter

def find(seq, predicate):
    return next(ifilter(predicate, seq), None)


def toBinaryString(num):
    return "{0:b}".format(num)


def fold_left(seq, zero, func):
    return reduce(func, [zero] + seq)

def flat_map(seq, func):
    mapped = map(func, seq)
    reduced = reduce(lambda a, b: a + [b], [[]] + mapped)
    return flatten_list(reduced)

def flatten_list(lst):
    def flat(acc, b):
        addition = b if isinstance(b, list) else [b]
        return acc + addition
    return reduce(flat, [[]] + lst)

def key_index_formats():
    from geotrellis.spark.io.index.KeyIndex import KeyIndex
    from geotrellis.spark.SpatialKey import SpatialKey
    from geotrellis.spark.io.json.KeyIndexFormats import ZSpatialKeyIndexFormat
    return {KeyIndex[SpatialKey]: ZSpatialKeyIndexFormat()}

def get_format_for_key_index_type(key_index_type):
    return key_index_formats()[key_index_type]

from avro.io import DatumReader, BinaryDecoder
from avro.datafile import DataFileReader
#import avro.schema 

import zlib
import json
import numpy
import cStringIO

def from_file(schema, file_path):
    with open(file_path, "rb") as file_compressed:
        decompressed = zlib.decompress(file_compressed.read())
        f = cStringIO.StringIO(decompressed)
        decoder = BinaryDecoder(f)
        datum_reader = DatumReader(schema)
        result = datum_reader.read(decoder)
        f.close()
        return result

def from_avro_file(file_path):
    with open(file_path, "rb") as f:
        with DataFileReader(f, DatumReader()) as reader:
            result = [rec for rec in reader]
            return result

def to_pairs(dct):
    return [Pair.from_dict(rec) for rec in dct['pairs']]

class Pair(object):
    def __init__(self, first, second):
        self._1 = first
        self._2 = second

    def is_suitable_dict(dct):
        return '_1' in dct

    @staticmethod
    def from_dict(dct):
        from geotrellis.spark.io.json.KeyFormats import SpatialKeyFormat
        pos = SpatialKeyFormat().from_dict(dct['_1'])
        bands = BandsContainer.from_dict(dct['_2'])
        return Pair(pos, bands)

class BandsContainer(object):
    def __init__(self, bands):
        self.bands = bands

    def is_suitable_dict(dct):
        return 'bands' in dct

    @staticmethod
    def from_dict(dct):
        bands = [Band.from_dict(rec) for rec in dct['bands']]
        return BandsContainer(bands)

class Band(object):
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

