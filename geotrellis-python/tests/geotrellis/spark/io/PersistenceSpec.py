from geotrellis.python.util.utils import fullname

class _PersistenceSpec(object):
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
        for keyIndexMethodName, keyIndexMethod in self.keyIndexMethod:
            result.append((keyIndexMethodName, keyIndexMethod, self.getLayerIds(keyIndexMethodName)))
        return result

    def persistence_spec_checks(self):
        for (keyIndexMethodName, keyIndexMethod,
                (layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId)) in self.specLayerIds:
            print("using key index method {mn}".format(mn=keyIndexMethodName))
            print("should not find layer before write")
            assert_raises(LayerNotFoundError, lambda: self.reader.read(K, V, M, layerId))
            print("should write a layer")
            self.writer.write(K, V, M, layerId, sample, keyIndexMethod)

            print("should read a layer back")
            actual = self.reader.read(K, V, M, layerId).keys().collect()
            expected = self.sample.keys().collect()
            assert Set(actual) == Set(expected)

            print("should read a single value")
            tileReader = self.tiles.reader(K, V, layerId)
            key = self.sample.keys().first()
            readV = tileReader.read(key)
            expectedV = self.sample.filter(lambda x:x[0] == key).values().first()
            assert readV == expectedV
