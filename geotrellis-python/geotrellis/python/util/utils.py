from __future__ import absolute_import
import os.path

_INTMIN = - (2 ** 31)
_INTMAX = - _INTMIN - 1

def isValidInt(num):
    return _INTMIN <= num <= _INTMAX

def file_exists(path):
    return os.path.isfile(path)

def fullname(typeObject):
    name = typeObject.__name__
    module = typeObject.__module__
    lastdot = module.rfind(".")
    withoutLast = module[:lastdot]
    return module + "." + name

def getOrElse(first, second):
    return first if first is not None else second

class JSONFormat(object):
    def get_fields(self, dct, *fieldnames):
        try:
            return map(lambda name: dct[name], fieldnames)
        except KeyError:
            return None

from itertools import ifilter

def find(seq, predicate):
    return next(ifilter(predicate, seq), None)


def toBinaryString(num):
    return "{0:b}".format(num)


def fold_left(seq, zero, func):
    return reduce(func, [zero] + seq)

def flat_map(seq, func):
    mapped = map(func, seq)
    return reduce(lambda a, b: a + [b], [[]] + mapped)

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
import avro.schema 

import zlib
import json
import numpy
import cStringIO

def from_file(schema, file_path):
    with open(file_path, "rb") as file_compressed:
        decompressed = zlib.decompress(file_compressed.read())
        with cStringIO.StringIO(decompressed) as f:
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

