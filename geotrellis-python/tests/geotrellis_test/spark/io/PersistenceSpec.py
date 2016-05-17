from __future__ import absolute_import
from geotrellis.python.util.utils import fullname
from geotrellis.spark.LayerId import LayerId
from geotrellis.spark.io.package_scala import LayerNotFoundError
from nose import tools

class _PersistenceSpecMeta(object):
    items = {}
    def __getitem__(self, key):
        if key in self.items.keys():
            return self.items[key]
        class tempo(_PersistenceSpecClass):
            K, V, M = key
        self.items[key] = tempo
        return tempo

_PersistenceSpec = _PersistenceSpecMeta()

#@tools.nottest
class _PersistenceSpecClass(object):
    def getLayerIds(self, keyIndexMethod):
        suffix = keyIndexMethod.replace(" ", "_")
        name = fullname(type(self))
        layerId = LayerId("sample-{name}-{suffix}".format(name=name,suffix=suffix), 1)
        deleteLayerId = LayerId("deleteSample-{name}-{suffix}".format(name=name,suffix=suffix), 1)
        copiedLayerId = LayerId("copySample-{name}-{suffix}".format(name=name,suffix=suffix), 1)
        movedLayerId = LayerId("moveSample-{name}-{suffix}".format(name=name,suffix=suffix), 1)
        reindexedLayerId = LayerId("reindexedSample-{name}-{suffix}".format(name=name,suffix=suffix), 1)
        return (layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId)

    @property
    def specLayerIds(self):
        result = []
        for keyIndexMethodName, keyIndexMethod in self.keyIndexMethods.items():
            result.append((keyIndexMethodName, keyIndexMethod, self.getLayerIds(keyIndexMethodName)))
        return result

    @tools.istest
    def test_persistence_spec_checks(self):
        K, V, M = self.K, self.V, self.M
        for (keyIndexMethodName, keyIndexMethod,
                (layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId)) in self.specLayerIds:
            print("using key index method {mn}".format(mn=keyIndexMethodName))
            print("should not find layer before write")
            tools.assert_raises(LayerNotFoundError, lambda: self.reader.read(K, V, M, layerId))
            print("should write a layer")
            self.writer.write(K, V, M, layerId, self.sample, keyIndexMethod)

            print("should read a layer back")
            actual = self.reader.read(K, V, M, layerId).keys().collect()
            expected = self.sample.keys().collect()
            assert set(actual) == set(expected)

            print("should read a single value")
            tileReader = self.tiles.reader(K, V, layerId)
            key = self.sample.keys().first()
            readV = tileReader.read(key)
            expectedV = self.sample.filter(lambda x:x[0] == key).values().first()
            assert readV == expectedV
