from __future__ import absolute_import
from geotrellis.spark.io.index.KeyIndex import KeyIndex
from geotrellis.spark.io.AttributeCaching import AttributeCaching
from geotrellis.python.util.utils import get_format_for_key_index_type
import avro.schema
import json

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

