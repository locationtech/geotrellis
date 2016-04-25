from avro.datafile import DataFileReader
from avro.io import DatumReader, BinaryDecoder
import avro.schema 

import zlib
import json
import tempfile
import numpy

def doit(path_to_landsat, zoom = 0, col = 0, row = 0):
    cat = path_to_landsat + '/data/catalog'
    flv = file_value_reader(cat)
    def reader(layerid):
       return flv.reader(SpatialKey, object, layerid)
     
    rdr = reader(LayerId("landsat", zoom))
    return rdr.read(SpatialKey(col, row))

def fetch_schema_string(file_path):
    attrs = read_attributes(file_path)
    return json.dumps(attrs[1]['schema'])

def read_attributes(file_path):
    with open(file_path, 'r') as f:
        return json.loads(f.read())

def from_file(schema, file_path):
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

class Pair(object):
    def __init__(self, first, second):
        self._1 = first
        self._2 = second

    def is_suitable_dict(dct):
        return '_1' in dct

    @staticmethod
    def from_dict(dct):
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

# the code below is geotrellis code ported to python
import os.path
from itertools import ifilter

class LayerIOError(Exception):
    def __init__(self, msg):
        self.msg = msg

class TileNotFoundError(LayerIOError):
    def __init__(self, key, layer_id):
        msg = ("Tile with key " + str(key) + 
            " not found for layer " + str(layer_id))
        LayerIOError.__init__(self, msg)

class AttributeNotFoundError(LayerIOError):
    def __init__(self, attr_name, layer_id):
        msg = ("Attribute " + attr_name + 
            " not found for layer " + str(layer_id))
        LayerIOError.__init__(self, msg)

class LayerNotFoundError(LayerIOError):
    def __init__(self, layer_id):
        msg = ("Layer " + str(layer_id) +
                " not found in the catalog")
        LayerIOError.__init__(self, msg)

# util function

def file_exists(path):
    return os.path.isfile(path)

# see geotrellis.spark.io.file.FileValueReader

def file_value_reader(first, second = None):
    def get_params():
        if second is None:
            if isinstance(first, FileAttributeStore):
                return first, first.catalog_path
            elif isinstance(first, str):
                return FileAttributeStore(first), first
            else:
                raise Exception("wrong params ({0}, {1})".format(str(first), str(second)))
        else:
            return first, second
    attribute_store, catalog_path = get_params()
    def reader(key_type, value_type, layer_id):
        header          = attribute_store.read_header(FileLayerHeader, layer_id)
        key_index       = attribute_store.read_key_index(key_type, layer_id)
        writer_schema   = attribute_store.read_schema(layer_id)

        max_width = Index.digits(key_index.to_index(key_index.key_bounds().max_key))
        key_path = generate_key_path_func(catalog_path, header.path, key_index, max_width)
        def read_func(key):
            path = key_path(key)
            if not file_exists(path):
               raise TileNotFoundError(key, layer_id)
            recs = to_pairs(from_file(writer_schema, path))
            found = find(recs, lambda pair: pair._1 == key)
            if found is None:
                raise TileNotFoundError(key, layer_id)
            else:
                return found._2
        return Reader(read_func)
    return ReaderGenerator(reader)

class Reader(object):
    def __init__(self, read_func):
        self.read_func = read_func

    def read(self, key):
        return self.read_func(key)

class ReaderGenerator(object):
    def __init__(self, reader_func):
        self.reader_func = reader_func

    def reader(self, key_type, value_type, layer_id):
        return self.reader_func(key_type, value_type, layer_id)

# util class

class JSONFormat(object):
    def get_fields(self, dct, *fieldnames):
        try:
            return map(lambda name: dct[name], fieldnames)
        except KeyError:
            return None

# see geotrellis.spark.LayerId

class LayerId(object):
    implicits = {'format': lambda: LayerIdFormat()}
    def __init__(self, name, zoom):
        self.name = name
        self.zoom = zoom

    def __str__(self):
        return "Layer(name=\"{0}\", zoom={1})".format(self.name, self.zoom)

    @staticmethod
    def from_tuple(tup):
        return LayerId(tup[0], tup[1])

# see geotrellis.spark.io.json.Implicits

class LayerIdFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'name', 'zoom')
        if not fields:
            raise DeserializationException("LayerId expected")
        name, zoom = fields
        return LayerId(name, zoom)

# see geotrellis.spark.io.file.KeyPathGenerator

def generate_key_path_func(catalog_path, layer_path, key_index, max_width):
    return lambda key: os.path.join(catalog_path, layer_path, Index.encode(key_index.to_index(key), max_width))
    
# util function

def find(seq, predicate):
    return next(ifilter(predicate, seq), None)

# see geotrellis.spark.io.index.Index
class Index(object):
    @staticmethod
    def encode(index, max_len):
        index_str = str(index)
        prefix = '0' * (max_len - len(index_str))
        return prefix + index_str

    @staticmethod
    def digits(x):
        if (x < 10):
            return 1
        else:
            return digits(x/10)

# see geotrellis.spark.io.AttributeCaching
class AttributeCaching(object):
    def __init__(self):
        self._cache = {}

    def cache_read(self, attr_type, layer_id, attr_name):
        tup = (layer_id, attr_name)
        if tup in self._cache:
            return self._cache[tup]
        else:
            attr = self.read(attr_type, layer_id, attr_name)
            self._cache[tup] = attr
            return attr

    def cache_write(self, attr_type, layer_id, attr_name, value):
        tup = (layer_id, attr_name)
        self._cache[tup] = value
        self.write(attr_type, layer_id, attr_name, value)

    def clear_cache(self, layer_id = None, attr_name = None):
        if layer_id is None:
            if attr_name is None:
                self._cache.clear()
            else:
                raise Exception("cannot clear attr when layer_id is unknown")
        else:
            if attr_name is None:
                for tup in filter(lambda each: each[0] == layer_id, self._cache):
                    del self._cache[tup]
            else:
                del self._cache[(layer_id, attr_name)]

# see geotrellis.spark.io.AttributeStore
class AttributeStore(AttributeCaching):
    def __init__(self):
        super(AttributeStore, self).__init__()
    def available_attrs(self, layer_id):
        pass

    def copy(self, from_layer_id, to_layer_id, attrs = None):
        if attrs is None:
            attrs = self.available_attrs(from_layer_id)
        for attr_name in attrs:
            self.write(to_layer_id, attr_name, self.read(from_layer_id, attr_name))

class Fields(object):
    metadata_blob = "metadata"
    header = "header"
    key_index = "keyIndex"
    metadata = "metadata"
    schema = "schema"

class LayerAttributes(object):
    def __init__(self, header, metadata, key_index, schema):
        self.header = header
        self.metadata = metadata
        self.key_index = key_index
        self.schema = schema

class BlobLayerAttributeStore(AttributeStore):
    def __init__(self):
        super(BlobLayerAttributeStore, self).__init__()
    def read_header(self, header_type, layer_id):
        header = self.cache_read(None, layer_id, Fields.metadata_blob)[Fields.header]
        header_format = header_type.implicits['format']()
        return header_format.from_dict(header)

    def read_metadata(self, metadata_type, layer_id):
        meta = self.cache_read(None, layer_id, Fields.metadata_blob)[Fields.metadata]
        metadata_format = metadata_type.implicits['format']()
        return metadata_format.from_dict(meta)

    def read_key_index(self, key_type, layer_id):
        key_index = self.cache_read(None, layer_id, Fields.metadata_blob)[Fields.key_index]
        # TODO temporary workaround. see geotrellis.spark.io.json.KeyIndexFormats
        key_index_format = get_format_for_key_index_type(KeyIndex[key_type])
        return key_index_format.from_dict(key_index)

    def read_schema(self, layer_id):
        schema_as_dict = self.cache_read(None, layer_id, Fields.metadata_blob)[Fields.schema]
        schema_json_string = json.dumps(schema_as_dict)
        return avro.schema.parse(schema_json_string)

    def read_layer_attrs(self, header_type, metadata_type, key_index_type, layer_id):
        # TODO get decoder from type (for example: header_type.implicits['format']() gives format)
        blob = self.cache_read(None, layer_id, Fields.metadata_blob)
        return LayerAttributes(
                    json.loads(blob[Fields.header], cls = self.header_decoder),
                    json.loads(blob[Fields.metadata], cls = self.metadata_decoder),
                    json.loads(blob[Fields.key_index], cls = self.key_index_decoder),
                    json.loads(blob[Fields.schema], cls = self.schema_decoder)
                    )

    def write_layer_attrs(self, header_type, metadata_type, key_index_type, layer_id, header, meta, key_index, schema):
        # TODO get encoder from type (for example: header_type.implicits['format']() gives format)
        jsobj = json.dumps({
                Fields.header:      json.dumps(header, cls = self.header_encoder),
                Fields.metadata:    json.dumps(meta, cls = self.metadata_encoder),
                Fields.key_index:   json.dumps(key_index, cls = self.key_index_encoder),
                Fields.schema:      json.dumps(schema, cls = self.schema_encoder)
            })
        self.cache_write(None, layer_id, Fields.metadata_blob, jsobj)

# see geotrellis.spark.io.file.FileAttributeStore
import re
import glob
import os

class FileAttributeStore(BlobLayerAttributeStore):

    def __init__(self, catalog_path):
        super(FileAttributeStore, self).__init__()
        self.catalog_path = catalog_path
        attr_dir = os.path.join(catalog_path, "attributes")
        if not os.path.exists(attr_dir):
            os.makedirs(attr_dir)
        self.attribute_directory = attr_dir

    SEP = "__.__"

    @staticmethod
    def attribute_rx(input_string):
        sep = FileAttributeStore.SEP
        slug = "[a-zA-Z0-9-]+"
        regex = "(" + slug + ")" + sep + "(" + slug + ")" + sep + "(" + slug + ").json"
        match = re.match(regex, input_string)
        if match:
            return match.groups()
        else:
            return None

    def attribute_file(self, layer_id, attr_name):
        sep = FileAttributeStore.SEP
        filename = layer_id.name + sep + str(layer_id.zoom) + sep + attr_name + ".json"
        return os.path.join(self.attribute_directory, filename)

    def attribute_files(self, layer_id):
        sep = FileAttributeStore.SEP
        def mapper(filepath):
            att = filepath.split(sep)[-1].replace(".json", "")
            return (att[0:-5], filepath)
        return map(mapper, self.layer_attribute_files(layer_id))

    # original name is read
    def read_file(self, attr_type, filepath):
        with open(filepath) as f:
            lst = json.loads(f.read())
            lst[0] = LayerIdFormat().from_dict(lst[0])
            if attr_type:
                attr_format = attr_type.implicits['format']()
                lst[1] = attr_format.from_dict(lst[1])
            return lst

    def read(self, attr_type, layer_id, attr_name):
        filepath = self.attribute_file(layer_id, attr_name)
        if not file_exists(filepath):
            raise AttributeNotFoundError(attr_name, layer_id)
        return self.read_file(attr_type, filepath)[1]

    def read_all(self, attr_type, attr_name):
        sep = FileAttributeStore.SEP
        filenames = glob.glob(self.attribute_directory + '/*' + sep + attr_name + '.json')
        return dict(self.read_file(attr_type, name) for name in filenames)

    def write(self, attr_type, layer_id, attr_name, value):
        filepath = self.attribute_file(layer_id, attr_name)
        # TODO encode layer_id and value (use layer_id_format and value_format)
        layer_id_format = LayerIdFormat()
        value_format = attr_type.implicits['format']()
        value = json.dumps(value)
        tup = (layer_id, value)
        with open(filepath, 'w') as f:
            f.write(json.dumps(tup))

    def layer_attribute_files(self, layer_id):
        sep = FileAttributeStore.SEP
        filter_str = (self.attribute_directory + '/' + layer_id.name + sep +
                layer_id.zoom + sep + '*.json')
        return glob.glob(filter_str)

    def layer_exists(self, layer_id):
        found_list = self.layer_attribute_files(layer_id)
        return bool(found_list)

    def delete(self, layer_id, attr_name = None):
        layer_files = self.layer_attribute_files(layer_id)
        if not layer_files:
            raise LayerNotFoundError(layer_id)
        if attr_name is None:
            for f in layer_files:
                os.remove(f)
            self.clear_cache(layer_id)
        else:
            found = find(layer_files, lambda f: f.endswith(sep + attr_name + '.json'))
            if found:
                os.remove(found)
            self.clear_cache(layer_id, attr_name)

    def layer_ids(self):
        sep = FileAttributeStore.SEP
        filenames = glob.glob(self.attribute_directory + '/*.json')
        def to_layer_id(f):
            splitted = f.split(sep)[:2]
            name     = splitted[0]
            zoom_str = splitted[1]
            return LayerId(name, int(zoom_str))
        ids = map(to_layer_id, filenames)
        return list(set(ids))

    def available_attributes(layer_id):
        layer_files = self.layer_attribute_files(layer_id)
        def to_attribute(filename):
            name, zoom, attr = FileAttributeStore.attribute_rx(filename)
            return attr
        return map(to_attribute, layer_files)

# see geotrellis.spark.io.index.zcurve

# util function

def to_binary_string(num):
    return "{0:b}".format(num)

class Z2(object):
    def __init__(self, x, y = None):
        if y is None:
            self.z = x
        else:
            self.z = Z2.split(x) | Z2.split(y) << 1

    def __lt__(self, other):
        return self.z < other.z

    def __le__(self, other):
        return self.z <= other.z

    def __gt__(self, other):
        return self.z > other.z

    def __ge__(self, other):
        return self.z >= other.z

    def __add__(self, offset):
        return Z2(self.z + offset)
    
    def __sub__(self, offset):
        return Z2(self.z - offset)

    def __eq__(self, other):
        return self.z == other.z

    def __ne__(self, other):
        return self.z != other.z

    def decode(self):
        return (Z2.combine(z), Z2.combine(z >> 1))

    def dim(self, i):
        return Z2.combine(z >> i)

    def mid(self, p):
        if p.z < self.z:
            return Z2(p.z + (self.z - p.z)/2)
        else:
            return Z2(self.z + (p.z - self.z)/2)

    def bitsToString(self):
        return "({0:16s})({1:8s},{2:8s})".format(
                to_binary_string(self.z),
                to_binary_string(self.dim(0)),
                to_binary_string(self.dim(1))
                )

    def __str__(self):
        return "" + z + self.decode()

    MAX_BITS = 31
    MAX_MASK = 0x7fffffff
    MAX_DIM = 2

    @staticmethod
    def split(value):
        x = value & Z2.MAX_MASK
        x = (x ^ (x << 32)) & 0x00000000ffffffffL
        x = (x ^ (x << 16)) & 0x0000ffff0000ffffL
        x = (x ^ (x <<  8)) & 0x00ff00ff00ff00ffL # 11111111000000001111111100000000..
        x = (x ^ (x <<  4)) & 0x0f0f0f0f0f0f0f0fL # 1111000011110000
        x = (x ^ (x <<  2)) & 0x3333333333333333L # 11001100..
        x = (x ^ (x <<  1)) & 0x5555555555555555L # 1010...
        return x

    @staticmethod
    def combine(z):
        x = z & 0x5555555555555555L
        x = (x ^ (x >>  1)) & 0x3333333333333333L
        x = (x ^ (x >>  2)) & 0x0f0f0f0f0f0f0f0fL
        x = (x ^ (x >>  4)) & 0x00ff00ff00ff00ffL
        x = (x ^ (x >>  8)) & 0x0000ffff0000ffffL
        x = (x ^ (x >> 16)) & 0x00000000ffffffffL 
        return x

    @staticmethod
    def unapply(instance):
        return instance.decode() # wrapped with Some in original version

    @staticmethod
    def zdivide(p, rmin, rmax):
        litmax, bigmin = zdiv(Z2.load, Z2.MAX_DIM)(p.z, rmin.z, rmax.z)
        return (Z2(litmax), Z2(bigmin))

    @staticmethod
    def load(target, p, bits, dim):
        mask = ~(Z2.split(Z2.MAX_MASK >> (Z2.MAX_BITS-bits)) << dim)
        wiped = target & mask
        return wiped | (Z2.split(p) << dim)

    @staticmethod
    def zranges(min_z2, max_z2):
        mq = MergeQueue()
        sr = Z2Range(min_z2, max_z2)

        rec_counter = 0
        report_counter = 0

        def _zranges(prefix, offset, quad):
            rec_counter += 1

            _min = prefix | (quad << offset)
            _max = _min | (1L << offset) - 1

            qr = Z2Range(Z2(_min), Z2(_max))
            if sr.contains(qr):
                mq += (qr.min.z, qr.max.z)
                report_counter += 1
            elif offset > 0 and sr.overlaps(qr):
                _zranges(min, offset - MAX_DIM, 0)
                _zranges(min, offset - MAX_DIM, 1)
                _zranges(min, offset - MAX_DIM, 2)
                _zranges(min, offset - MAX_DIM, 3)

        prefix = 0
        offset = Z2.MAX_BITS * Z2.MAX_DIM
        _zranges(prefix, offset, 0)
        mq.toSeq

# see package.scala in geotrellis.spark.io.index
def zdiv(load_func, dims):
    def inner(xd, rmin, rmax):
        if rmin >= rmax:
            raise Exception("min ({0}) must be less than max ({1})".format(rmin, rmax))
        zmin = rmin
        zmax = rmax
        bigmin = 0L
        litmax = 0L

        def bit(x, idx):
            return (x & (1L << idx)) >> idx

        def over(bits):
            return 1L << (bits-1)

        def under(bits):
            return (1L << (bits-1)) - 1

        i = 64
        while i > 0:
            i -= 1
            bits = i/dims+1
            dim = i%dims

            a,b,c = bit(xd, i), bit(zmin, i), bit(zmax, i)
            if a == 0 and b == 0 and c == 0:
                # continue
                pass
            elif a == 0 and b == 0 and c == 1:
                zmax    = load_func(zmax, under(bits), bits, dim)
                bigmin  = load_func(zmin, over(bits), bits, dim)
            elif a == 0 and b == 1 and c == 0:
                #  sys.error(s"Not possible, MIN <= MAX, (0, 1, 0)  at index $i")
                pass
            elif a == 0 and b == 1 and c == 1:
                bigmin = zmin
                return (litmax, bigmin)
            elif a == 1 and b == 0 and c == 0:
                litmax = zmax
                return (litmax, bigmin)
            elif a == 1 and b == 0 and c == 1:
                litmax  = load(zmax, under(bits), bits, dim)
                zmin    = load(zmin, over(bits), bits, dim)
            elif a == 1 and b == 1 and c == 0:
                #  sys.error(s"Not possible, MIN <= MAX, (1, 1, 0) at index $i")
                pass
            elif a == 1 and b == 1 and c == 1:
                # continue
                pass
        return (litmax, bigmin)

    return inner

# see georellis.spark.io.index.MergeQueue

import sys
import bisect

class MergeQueue(object):
    def __init__(self, initial_size = 1, lt_func = None):
        self.initial_size = initial_size
        self._lt_func = lt_func
        self._array = []
        self._size = 0

    def size(self):
        return self._size

    def _remove_element(self, i):
        del self._array[i]
        self._size -= 1

    def _insert_element(_range, i):
        self._ensure_size(self._size + 1)
        if i == self._size:
            self._array[i] = _range
        else:
            self._array = self._array[:i] + [_range] + self._array[i:]
        self._size += 1

    def _ensure_size(n):
        pass

    def __iadd__(self, _range):
        res = -1
        if self._size != 0:
            lt_func = self._lt_func
            haystack = self._array if lt_func is None else CustomComparatorSeq(self._array, lt_func)
            needle = _range if lt_func is None else CustomComparator(_range, lt_func)
            res = bisect.bisect_left(haystack, needle)
        if res < 0:
            i = -(res + 1)
            this_start, this_end = _range
            remove_left = False
            remove_right = False
            right_remainder = None

            if i != 0:
                prev_start, prev_end = self._array[i-1]
                if prev_start == this_start:
                    remove_left = True
                if prev_end + 1 >= this_start:
                    remove_left = True
                    this_start = prev_start
                    if prev_end > this_end:
                        this_end = prev_end
            if i < self._size and self._size > 0:
                next_start, next_end = self._array[i]
                if this_start == next_start:
                    remove_right = True
                    this_end = next_end
                else:
                    if this_end + 1 >= next_start:
                        remove_right = True
                        if next_end - 1 >= this_end:
                            this_end = next_end
                        elif next_end < this_end - 1:
                            right_remainder = (next_end+1, this_end)
                            this_end = next_end
            if remove_right:
                if not remove_left:
                    self._array[i] = (this_start, this_end)
                else:
                    self._array[i-1] = (this_start, this_end)
                    self._remove_element(i)
            elif remove_left:
                self._array[i-1] = (this_start, this_end)
            else:
                self._insert_element(_range, i)
            if right_remainder is not None:
                self += right_remainder
        return self
    
    def to_seq(self):
        return self._array[:]

# util functions

class CustomComparator(object):
    def __init__(self, value, lt_func):
        self.value = value
        self.lt_func = lt_func
    def __lt__(self, other):
        return self.lt_func(self.value, other)

class CustomComparatorSeq(object):
    def __init__(self, seq, lt_func):
        self.seq = seq
        self.lt_func = lt_func
    def __getitem__(self, key):
        return CustomComparator(self.seq[key], lt_func)
    def __len__(self):
        return len(self.seq)

# see geotrellis.spark.io.index.zcurve.Z2Range
class Z2Range(object):
    def __init__(self, _min, _max):
        if _min.z > _max.z:
            raise Exception("NOT: {0} < {1}".format(_min.z, _max.z))
        self.min = _min
        self.max = _max

    def mid(self):
        _min = self.min
        _max = self.max
        return _min.mid(_max)

    def length(self):
        return self.max.z - self.min.z

    def contains(self, z):
        if isinstance(z, Z2):
            x,y = z.decode()
            _min = self.min
            _max = self.max
            return (x >= _min.dim(0) and
                    x <= _max.dim(0) and
                    y >= _min.dim(1) and
                    y <= _max.dim(1))
        else:
            return self.contains(z.min) and self.contains(z.max)

    def overlaps(self, r):
        def _overlaps(a1, a2, b1, b2):
            return max(a1, b1) <= min(a2, b2)
        return (_overlaps(self.min.dim(0), self.max.dim(0), r.min.dim(0), r.max.dim(0)) and 
                _overlaps(self.min.dim(1), self.max.dim(1), r.min.dim(1), r.max.dim(1)))

    def cut(self, xd, in_range):
        if self.min.z == self.max.z:
            return []
        elif in_range:
            if xd.z == self.min.z:
                return [Z2Range(self.max, self.max)]
            elif xd.z == self.max.z:
                return [Z2Range(self.min, self.min)]
            else:
                return [Z2Range(self.min, xd-1), Z2Range(xd+1, self.max)]
        else:
            litmax, bigmin = Z2.zdivide(xd, self.min, self.max)
            return [Z2Range(self.min, litmax), Z2Range(bigmin, self.max)]

# see geotrellis.spark.Boundable
class Boundable(object):
    def __init__(self, min_bound_func, max_bound_func):
        self.min_bound_func = min_bound_func
        self.max_bound_func = max_bound_func

    def min_bound(self, p1, p2):
        return self.min_bound_func(p1, p2)

    def max_bound(self, p1, p2):
        return self.max_bound_func(p1, p2)

    def include(self, p, bounds):
        return KeyBounds(
                self.min_bound(bounds.min_key, p),
                self.max_bound(bounds.max_key, p))

    def includes(self, p, bounds):
        return bounds == self.include(p, bounds)

    def combine(self, b1, b2):
        return KeyBounds(
                self.min_bound(b1.min_key, b2.min_key),
                self.max_bound(b2.max_key, b2.max_key))

    def intersect(self, b1, b2):
        kb = KeyBounds(
                self.max_bound(b1.min_key, b2.min_key),
                self.min_bound(b2.max_key, b2.max_key))

        if self.min_bound(kb.min_key, kb.max_key) == kb.min_key:
            return kb
        else:
            return None

    def intersects(self, b1, b2):
        return self.intersect(b1, b2) is not None

# see geotrellis.spark.SpatialKey

class SpatialKey(object):
    implicits = {'format': lambda: SpatialKeyFormat()}
    def __init__(self, col, row):
        self.col = col
        self.row = row
    def __getitem__(self, index):
        if index == 0:
            return self.col
        elif index == 1:
            return self.row
        else:
            raise Exception("Index {0} is out of [0,1] bounds.".format(index))
    def __lt__(self, other):
        return SpatialKey.to_tuple(self) < SpatialKey.to_tuple(other)

    def __eq__(self, other):
        if not isinstance(other, SpatialKey):
            return False
        return (self.col == other.col and
                self.row == other.row)
    def __hash__(self):
        return hash(tuple(col, row))

    @staticmethod
    def to_tuple(key):
        return (key.col, key.row)

    @staticmethod
    def from_tuple(tup):
        col, row = tup
        return SpatialKey(col, row)

    boundable = Boundable(
            lambda a, b: SpatialKey(min(a.col, b.col), min(a.row, b.row)),
            lambda a, b: SpatialKey(max(a.col, b.col), max(a.row, b.row)))

# see geotrellis.util.GetComponent
class GetComponent(object):
    def get(self):
        pass

def get_component(_get):
    class TempGetComponent(GetComponent):
        def get(self):
            return _get
    return TempGetComponent()

# see geotrellis.util.SetComponent
class SetComponent(object):
    def set(self):
        pass

def set_component(_set):
    class TempSetComponent(SetComponent):
        def set(self):
            return _set
    return TempSetComponent()

# see geotrellis.util.Component
class Component(GetComponent, SetComponent):
    pass

def component(_get, _set):
    class TempComponent(Component):
        def get(self):
            return _get
        def set(self):
            return _set
    return TempComponent()

# see geotrellis.spark.KeyBounds
class Bounds(object):
    def is_empty(self):
        pass

    def non_empty(self):
        return not self.is_empty()

    def intersect(self, other, b):
        pass

    def intersects(self, other, b):
        return self.intersect(other, b).non_empty()

    def get(self):
        pass

    def get_or_else(self, default_func):
        if self.is_empty():
            return default_func()
        else:
            return self.get()

    def map(self, func):
        if self.is_empty():
            return EmptyBounds
        else:
            return func(self.get())

    def flat_map(self, func):
        if self.is_empty():
            return EmptyBounds
        else:
            return func(self.get())

    @staticmethod
    def from_rdd(rdd):
        return rdd.map(lambda k, tile: bounds(k, k)).fold(EmptyBounds, lambda a, b: a.combine(b))

    @staticmethod
    def to_iterable(b):
        if b is EmptyBounds:
            return []
        else:
            return [b]

def bounds(_min, _max):
    return KeyBounds(_min, _max)

class TempEmptyBounds(Bounds):
    def is_empty(self):
        return True

    def include(self, key, b = None):
        return KeyBounds(key, key)

    def includes(self, key, b = None):
        return False

    def combine(self, other, b = None):
        return other

    def contains(self, other, b = None):
        return False
    
    def intersect(self, other, b = None):
        return EmptyBounds

    def get(self):
        raise Exception("EmptyBounds.get") # TODO NoSuchElementException analog

    def get_spatial_bounds(self, other, ev = None):
        return self

EmptyBounds = TempEmptyBounds()

class _KeyBoundsMeta(object):
    items = {}
    def __getitem__(self, key_type):
        if key_type in self.items.keys():
            return self.items[key_type]
        class tempo(_KeyBounds):
            implicits = {'format': lambda: KeyBoundsFormat(key_type.implicits['format']())}
        self.items[key_type] = tempo
        return tempo
    # TODO code duplication (see _KeyBounds.__init__)
    def __call__(self, min_key, max_key = None):
        if max_key is None:
            if isinstance(min_key, GridBounds):
                grid_bounds = min_key
                min_key = SpatialKey(grid_bounds.col_min, grid_bounds.row_min)
                max_key = SpatialKey(grid_bounds.col_max, grid_bounds.row_max)
            else:
                raise Exception("wrong arguments ({0}) passed to KeyBounds constructor.".format(min_key))
        key_type = type(min_key)
        return self[key_type](min_key, max_key)

KeyBounds = _KeyBoundsMeta()

class _KeyBounds(Bounds):
    def __init__(self, min_key, max_key = None):
        if max_key is None:
            if isinstance(min_key, GridBounds):
                grid_bounds = min_key
                min_key = SpatialKey(grid_bounds.col_min, grid_bounds.row_min)
                max_key = SpatialKey(grid_bounds.col_max, grid_bounds.row_max)
            else:
                raise Exception("wrong arguments ({0}) passed to KeyBounds constructor.".format(min_key))
        self.min_key = min_key
        self.max_key = max_key

    def is_empty(self):
        return False

    def include(self, key, b):
        return KeyBounds(b.min_bound(self.min_key, key), b.max_bound(self.max_key, key))

    def includes(self, key, b):
        return self.min_key == b.min_bound(self.min_key, key) and self.max_key == b.max_bound(self.max_key, key)
    
    def combine(self, other, b):
        if other is EmptyBounds:
            return self
        else:
            new_min = b.min_bound(self.min_key, other.min_key)
            new_max = b.max_bound(self.max_key, other.max_key)
            return KeyBounds(new_min, new_max)

    def contains(self, other, b):
        if other is EmptyBounds:
            return True
        else:
            return self.min_key == b.min_bound(self.min_key, other.min_key) and self.max_key == b.max_bound(self.max_key, other.max_key)

    def intersect(self, other, b):
        if other is EmptyBounds:
            return EmptyBounds
        else:
            new_min = b.max_bound(self.min_key, other.min_key)
            new_max = b.min_bound(self.max_key, other.max_key)

            if b.min_bound(new_min, new_max) == new_min:
                return KeyBounds(new_min, new_max)
            else:
                return EmptyBounds

    def get(self):
        return self

    def set_spatial_bounds(self, other, ev):
        if isinstance(other, GridBounds):
            other = KeyBounds(
                        SpatialKey(other.col_min, other.row_min),
                        SpatialKey(other.col_max, other.row_max))
        new_min = ev.set()(self.min_key, other.min_key)
        new_max = ev.set()(self.max_key, other.max_key)
        return KeyBounds(new_min, new_max)
    
    @staticmethod
    def include_key(seq, key):
        mapped = map(lambda kb: kb.includes(key), seq)
        return reduce(lambda a, b: a or b, mapped)

    @staticmethod
    def to_tuple(key_bounds):
        return (key_bounds.min_key, key_bounds.max_key)

    def to_grid_bounds(self):
        min_col = self.min_key.col
        min_row = self.min_key.row
        max_col = self.max_key.col
        max_row = self.max_key.row
        return GridBounds(min_col, min_row, max_col, max_row)

# see geotrellis.raster.GridBounds

class GridBounds(object):
    def __init__(self, colmin, rowmin, colmax, rowmax):
        self.col_min = colmin
        self.row_min = rowmin
        self.col_max = colmax
        self.row_max = rowmax

    def width(self):
        return self.col_max - self.col_min + 1

    def height(self):
        return self.row_max - self.row_min + 1

    def size(self):
        return self.width() * self.height()

    def is_empty(self):
        return self.size() == 0

    def contains(self, col, row):
        return ((self.col_min <= col and col <= self.col_max) and 
                (self.row_min <= row and row <= self.row_max))

    def intersects(self, other):
        return (not (self.col_max < other.col_min or other.col_max < self.col_min) and
                not (self.row_max < other.row_min or other.row_max < self.row_min))

    def __sub__(self, other):
        return self.minus(other)

    def minus(self, other):
        if not self.intersects(other):
            return [self]

        overlap_col_min = max(self.col_min, other.col_min)
        overlap_col_max = min(self.col_max, other.col_max)
        overlap_row_min = max(self.row_min, other.row_min)
        overlap_row_max = min(self.row_max, other.row_max)

        result = []

        if self.col_min < overlap_col_min:
            result.append(GridBounds(self.col_min, self.row_min, overlap_col_min-1, self.row_max))

        if overlap_col_max < self.col_max:
            result.append(GridBounds(overlap_col_max+1, self.row_min, self.col_max, self.row_max))

        if self.row_min < overlap_row_min:
            result.append(GridBounds(overlap_col_min, self.row_min, overlap_col_max, overlap_row_min-1))

        if overlap_row_max < self.row_max:
            result.append(GridBounds(overlap_col_min, overlap_row_max+1, overlap_col_max, self.row_max))

        return result

    def coords(self):
        arr = [None] * self.size()
        for row in xrange(0, self.height()):
            for col in xrange(0, self.width()):
                index = row*self.width() + col
                arr[index] = (col + self.col_min, row + self.row_min)
        return arr

    def intersection(self, other):
        if isinstance(other, CellGrid):
            other = grid_bounds(other)
        if not self.intersects(other):
            return None
        return GridBounds(
                max(self.col_min, other.col_min),
                max(self.row_min, other.row_min),
                min(self.col_max, other.col_max),
                min(self.row_max, other.row_max))

    @staticmethod
    def envelope(keys):
        col_min = sys.maxint
        col_max = -(sys.maxint + 1)
        row_min = sys.maxint
        row_max = -(sys.maxint + 1)

        for key in keys:
            col = key[0]
            row = key[1]
            if col < col_min:
                col_min = col
            if col > col_max:
                col_max = col
            if row < row_min:
                row_min = row
            if row > row_max:
                row_max = row

        return GridBounds(col_min, row_min, col_max, row_max)

    @staticmethod
    def distinct(grid_bounds_list):
        def func(acc, bounds):
            def inner_func(cuts, bounds):
                return flat_map(cuts, lambda a: a - bounds)
            acc + fold_left(acc, [bounds], inner_func)
        return fold_left(grid_bounds_list, [], func)

def grid_bounds(cellgrid):
    return GridBounds(0, 0, cellgrid.cols-1, cellgrid.rows-1)

# util functions

def fold_left(seq, zero, func):
    return reduce(func, [zero] + seq)

def flat_map(seq, func):
    mapped = map(func, seq)
    return reduce(lambda a, b: a + [b], [[]] + mapped)

# see geotrellis.raster.CellGrid

class CellGrid(object):
    def cols(self):
        pass
    def rows(self):
        pass
    def cell_type(self):
        pass
    def dimensions(self):
        return (self.cols, self.rows)
    def grid_bounds(self):
        return GridBounds(0, 0, self.cols()-1, self.rows()-1)
    def size(self):
        return self.cols() * self.rows()

# see geotrellis.spark.io.index.KeyIndex

class _KeyIndexMeta(object):
    items = {}
    def __getitem__(self, key):
        if key in self.items:
            return self.items[key]
        class tempo(_KeyIndex):
            pass
        self.items[key] = tempo
        return tempo

KeyIndex = _KeyIndexMeta()

class _KeyIndex(object):
    def key_bounds(self):
        pass
    def to_index(self, key):
        pass
    def index_ranges(self, key_range):
        pass

# see geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex

class ZSpatialKeyIndex(KeyIndex[SpatialKey]):
    implicits = {'format': lambda: ZSpatialKeyIndexFormat()}
    def __init__(self, key_bounds):
        self._key_bounds = key_bounds
    def key_bounds(self):
        return self._key_bounds
    def _toz(self, key):
        return Z2(key.col, key.row)
    def to_index(self, key):
        return self._toz(key).z
    def index_ranges(self, key_range):
        return Z2.zranges(self._toz(key_range[0]), self._toz(key_range[1]))

# see geotrellis.spark.io.LayerHeader

class LayerHeader(object):
    implicits = {'format': lambda: LayerHeaderFormat()}
    def format(self):
        pass
    def key_class(self):
        pass
    def value_class(self):
        pass

class LayerHeaderFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'format', 'keyClass', 'valueClass')
        if not fields:
            raise DeserializationException("LayerHeader expected")
        _format, _key_class, _value_class = fields
        class TempLayerHeader(LayerHeader):
            def format(self):
                return _format
            def key_class(self):
                return _key_class
            def value_class(self):
                return _value_class
        return TempLayerHeader()

# see geotrellis.spark.io.file.FileLayerHeader

class FileLayerHeader(LayerHeader):
    implicits = {'format': lambda: FileLayerHeaderFormat()}
    def __init__(self, _key_class, _value_class, path):
        self._key_class = _key_class
        self._value_class = _value_class
        self.path = path
    def format(self):
        return 'file'
    def key_class(self):
        return self._key_class
    def value_class(self):
        return self._value_class

class FileLayerHeaderFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'keyClass', 'valueClass', 'path')
        if not fields:
            raise DeserializationException("FileLayerHeader expected")
        key_class, value_class, path = fields
        return FileLayerHeader(key_class, value_class, path)

# see package.scala in spray.json

class DeserializationException(Exception):
    def __init__(self, msg, cause = None, field_names = []):
        self.msg = msg
        self.cause = cause

# see geotrellis.spark.io.json.KeyIndexFormats

class ZSpatialKeyIndexFormat(JSONFormat):
    TYPE_NAME = "zorder"

    def from_dict(self, dct):
        fields = self.get_fields(dct, 'type', 'properties')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex type:" +
                    " ZSpatialKeyIndex expected.")
        typename, props = fields
        if typename != ZSpatialKeyIndexFormat.TYPE_NAME:
            raise DeserializationException(
                    "Wrong KeyIndex type: {0} expected.".format(
                        ZSpatialKeyIndexFormat.TYPE_NAME))
        fields = self.get_fields(props, 'keyBounds')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex constructor arguments:" +
                    " ZSpatialKeyIndex constructor arguments expected.")
        key_bounds_dict = fields[0]
        key_bounds = KeyBoundsFormat(SpatialKeyFormat()).from_dict(key_bounds_dict)
        return ZSpatialKeyIndex(key_bounds)

# util function

key_index_formats = {KeyIndex[SpatialKey]: ZSpatialKeyIndexFormat()}

def get_format_for_key_index_type(key_index_type):
    return key_index_formats[key_index_type]

# see geotrellis.spark.io.json.KeyFormats

class KeyBoundsFormat(JSONFormat):
    def __init__(self, keyformat):
        self.keyformat = keyformat
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'minKey', 'maxKey')
        if not fields:
            raise DeserializationException(
                    "minKey, maxKey properties expected")
        minkey, maxkey = fields
        return KeyBounds(
                self.keyformat.from_dict(minkey),
                self.keyformat.from_dict(maxkey))

class SpatialKeyFormat(JSONFormat):
    def from_dict(self, dct):
        fields = self.get_fields(dct, 'col', 'row')
        if not fields:
            raise DeserializationException("SpatialKey expected. got: " + json.dumps(dct))
        col, row = fields
        return SpatialKey(col, row)
