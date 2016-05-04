from geotrellis.spark.io.AttributeStore import BlobLayerAttributeStore
from geotrellis.spark.io.json.Implicits import LayerIdFormat
from geotrellis.spark.io.package_scala import AttributeNotFoundError, LayerNotFoundError
from geotrellis.spark.LayerId import LayerId
from geotrellis.python.util.utils import file_exists
import re
import glob
import os
import os.path
import json

sep = "__.__"

class FileAttributeStore(BlobLayerAttributeStore):

    def __init__(self, catalog_path):
        super(FileAttributeStore, self).__init__()
        self.catalogPath = catalog_path
        attr_dir = os.path.join(catalog_path, "attributes")
        if not os.path.exists(attr_dir):
            os.makedirs(attr_dir)
        self.attributeDirectory = attr_dir

    SEP = "__.__"

    @staticmethod
    def attributeRx(input_string):
        slug = "[a-zA-Z0-9-]+"
        regex = "(" + slug + ")" + sep + "(" + slug + ")" + sep + "(" + slug + ").json"
        match = re.match(regex, input_string)
        if match:
            return match.groups()
        else:
            return None

    def attributeFile(self, layer_id, attr_name):
        filename = layer_id.name + sep + str(layer_id.zoom) + sep + attr_name + ".json"
        return os.path.join(self.attributeDirectory, filename)

    def attributeFiles(self, layer_id):
        def mapper(filepath):
            att = filepath.split(sep)[-1].replace(".json", "")
            return (att[0:-5], filepath)
        return map(mapper, self.layerAttributeFiles(layer_id))

    def read(self, attr_type, layer_id, attr_name = None):
        if attr_name is None:
            filepath = layer_id
            with open(filepath) as f:
                lst = json.loads(f.read())
                lst[0] = LayerIdFormat().from_dict(lst[0])
                if attr_type:
                    attr_format = attr_type.implicits['format']()
                    lst[1] = attr_format.from_dict(lst[1])
                return lst
        else:
            filepath = self.attributeFile(layer_id, attr_name)
            if not file_exists(filepath):
                raise AttributeNotFoundError(attr_name, layer_id)
            return self.read(attr_type, filepath)[1]

    def readAll(self, attr_type, attr_name):
        filenames = glob.glob(self.attributeDirectory + '/*' + sep + attr_name + '.json')
        return dict(self.read(attr_type, name) for name in filenames)

    def write(self, attr_type, layer_id, attr_name, value):
        filepath = self.attributeFile(layer_id, attr_name)
        # TODO encode layer_id and value (use layer_id_format and value_format)
        layer_id_format = LayerIdFormat()
        value_format = attr_type.implicits['format']()
        value = json.dumps(value)
        tup = (layer_id, value)
        with open(filepath, 'w') as f:
            f.write(json.dumps(tup))

    def layerAttributeFiles(self, layer_id):
        filter_str = (self.attributeDirectory + '/' + layer_id.name + sep +
                layer_id.zoom + sep + '*.json')
        return glob.glob(filter_str)

    def layerExists(self, layer_id):
        found_list = self.layerAttributeFiles(layer_id)
        return bool(found_list)

    def delete(self, layer_id, attr_name = None):
        layer_files = self.layerAttributeFiles(layer_id)
        if not layer_files:
            raise LayerNotFoundError(layer_id)
        if attr_name is None:
            for f in layer_files:
                os.remove(f)
            self.clearCache(layer_id)
        else:
            found = find(layer_files, lambda f: f.endswith(sep + attr_name + '.json'))
            if found:
                os.remove(found)
            self.clearCache(layer_id, attr_name)

    @property
    def layerIds(self):
        filenames = glob.glob(self.attributeDirectory + '/*.json')
        def to_layer_id(f):
            splitted = f.split(sep)[:2]
            name     = splitted[0]
            zoom_str = splitted[1]
            return LayerId(name, int(zoom_str))
        ids = map(to_layer_id, filenames)
        return list(set(ids))

    def availableAttributes(layer_id):
        layer_files = self.layerAttributeFiles(layer_id)
        def to_attribute(filename):
            name, zoom, attr = FileAttributeStore.attributeRx(filename)
            return attr
        return map(to_attribute, layer_files)

