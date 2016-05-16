from __future__ import absolute_import
from spec import Spec, skip
from nose import tools
from nose.tools import ok_
from tests.geotrellis_test.spark.TestEnvironment import _TestEnvironment
from geotrellis.python.util.utils import JSONFormat
from geotrellis.spark.LayerId import LayerId
from geotrellis.python.spray.json.package_scala import DeserializationException

@tools.nottest
class _AttributeStoreSpec(_TestEnvironment):
    @property
    def attributeStore(self):
        pass

    @tools.istest
    def test_01_should_write_to_an_attribute_store(self):
        """should write to an attribute store"""
        store = self.attributeStore
        store.write(str, LayerId("test1", 1), "test-att1", "test")
        store.write(str, LayerId("test2", 2), "test-att1", "test")
        store.write(str, LayerId("test2", 2), "test-att2", "test")
        store.write(str, LayerId("test3", 3), "test-att1", "test")

    @tools.istest
    def test_02_should_know_that_these_new_IDs_exist(self):
        """should know that these new IDs exist"""
        store = self.attributeStore
        ok_(store.layerExists(LayerId("test1", 1)) == True)
        ok_(store.layerExists(LayerId("test2", 2)) == True)
        ok_(store.layerExists(LayerId("test3", 3)) == True)

    @tools.istest
    def test_03_should_read_layer_ids(self):
        """should read layer ids"""
        store = self.attributeStore
        layerids = sorted(store.layerIds(), key = lambda layerid: layerid.zoom)
        ok_(layerids == [LayerId("test1", 1), LayerId("test2", 2), LayerId("test3", 3)])

    @tools.istest
    def test_04_should_clear_out(self):
        """should clear out the attribute store"""
        store = self.attributeStore
        for layerid in store.layerIds():
            store.delete(layerid)

        length = len(store.layerIds())
        ok_(length == 0)

    @tools.istest
    def test_should_save_and_load_a_random_object(self):
        """should save and load a random object convertable to/from json"""
        layerid = LayerId("test", 3)
        class Foo(object):
            implicits = {"format": lambda: FooFormat()}
            def __init__(self, x, y):
                self.x = x
                self.y = y
            def __eq__(self, other):
                if not isinstance(other, Foo):
                    return False
                return self.x == other.x and self.y == other.y
        class FooFormat(JSONFormat):
            def to_dict(self, obj):
                return {"first": obj.x, "second": obj.y}

            def from_dict(self, dct):
                fields = self.get_fields(dct, "first", "second")
                if not fields:
                    raise DeserializationException("JSON reading failed")
                x, y = fields
                return Foo(x, y)

        foo = Foo(1, "thing")
        store = self.attributeStore
        store.write(Foo, layerid, "foo", foo)
        fromstore = store.read(Foo, layerid, "foo")
        ok_(fromstore == foo)

