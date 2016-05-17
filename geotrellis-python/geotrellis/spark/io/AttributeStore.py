from __future__ import absolute_import
from __future__ import absolute_import
from geotrellis.spark.io.index.KeyIndex import KeyIndex
from geotrellis.spark.io.AttributeCaching import AttributeCaching
from geotrellis.python.util.utils import get_format_for_key_index_type, SchemaFormat
import avro.schema
import json

class AttributeStore(AttributeCaching):
    def __init__(self):
        super(AttributeStore, self).__init__()
    def availableAttributes(self, layer_id):
        pass

    def copy(self, from_layer_id, to_layer_id, attrs = None):
        if attrs is None:
            attrs = self.availableAttributes(from_layer_id)
        for attr_name in attrs:
            self.write(to_layer_id, attr_name, self.read(from_layer_id, attr_name))

class Fields(object):
    metadataBlob = "metadata"
    header = "header"
    keyIndex = "keyIndex"
    metadata = "metadata"
    schema = "schema"

class LayerAttributes(object):
    def __init__(self, header, metadata, keyIndex, schema):
        self.header = header
        self.metadata = metadata
        self.keyIndex = keyIndex
        self.schema = schema

class BlobLayerAttributeStore(AttributeStore):
    def __init__(self):
        super(BlobLayerAttributeStore, self).__init__()
    def readHeader(self, header_type, layer_id):
        header = self.cacheRead(dict, layer_id, Fields.metadataBlob)[Fields.header]
        header_format = header_type.implicits['format']()
        return header_format.from_dict(header)

    def readMetadata(self, metadata_type, layer_id):
        meta = self.cacheRead(dict, layer_id, Fields.metadataBlob)[Fields.metadata]
        metadata_format = metadata_type.implicits['format']()
        return metadata_format.from_dict(meta)

    def readKeyIndex(self, key_type, layer_id):
        key_index = self.cacheRead(dict, layer_id, Fields.metadataBlob)[Fields.keyIndex]
        # TODO temporary workaround. see geotrellis.spark.io.json.KeyIndexFormats
        #key_index_format = get_format_for_key_index_type(KeyIndex[key_type])
        key_index_format = KeyIndex[key_type].implicits['format']()
        return key_index_format.from_dict(key_index)

    def readSchema(self, layer_id):
        schema_as_dict = self.cacheRead(dict, layer_id, Fields.metadataBlob)[Fields.schema]
        schema_json_string = json.dumps(schema_as_dict)
        return avro.schema.parse(schema_json_string)

    def readLayerAttributes(self, header_type, metadata_type, key_type, layer_id):
        headerFormat =    header_type.implicits['format']()
        metaFormat =    metadata_type.implicits['format']()
        keyindFormat = KeyIndex[key_type].implicits['format']()
        schemaFormat = SchemaFormat()
        blob = self.cacheRead(dict, layer_id, Fields.metadataBlob)
        return LayerAttributes(
                    headerFormat.from_dict(blob[Fields.header]),
                    metaFormat.from_dict(blob[Fields.metadata]),
                    keyindFormat.from_dict(blob[Fields.keyIndex]),
                    schemaFormat.from_dict(blob[Fields.schema]))

    def writeLayerAttributes(self, header_type, metadata_type, key_index_type, layer_id, header, meta, key_index, schema):
        headerFormat =    header_type.implicits['format']()
        metaFormat =    metadata_type.implicits['format']()
        keyindFormat = key_index_type.implicits['format']()
        schemaFormat = SchemaFormat()
        jsobj = {
                Fields.header:      headerFormat.to_dict(header),
                Fields.metadata:    metaFormat.to_dict(meta),
                Fields.keyIndex:    keyindFormat.to_dict(key_index),
                Fields.schema:      schemaFormat.to_dict(schema)
            }
        self.cacheWrite(dict, layer_id, Fields.metadataBlob, jsobj)

