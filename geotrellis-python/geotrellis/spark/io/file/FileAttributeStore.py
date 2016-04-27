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
        self.catalog_path = catalog_path
        attr_dir = os.path.join(catalog_path, "attributes")
        if not os.path.exists(attr_dir):
            os.makedirs(attr_dir)
        self.attribute_directory = attr_dir

    SEP = "__.__"

    @staticmethod
    def attribute_rx(input_string):
        slug = "[a-zA-Z0-9-]+"
        regex = "(" + slug + ")" + sep + "(" + slug + ")" + sep + "(" + slug + ").json"
        match = re.match(regex, input_string)
        if match:
            return match.groups()
        else:
            return None

    def attribute_file(self, layer_id, attr_name):
        filename = layer_id.name + sep + str(layer_id.zoom) + sep + attr_name + ".json"
        return os.path.join(self.attribute_directory, filename)

    def attribute_files(self, layer_id):
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

