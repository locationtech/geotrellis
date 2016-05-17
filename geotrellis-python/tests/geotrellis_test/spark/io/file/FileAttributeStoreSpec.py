from __future__ import absolute_import
from tests.geotrellis_test.spark.io.AttributeStoreSpec import _AttributeStoreSpec
from geotrellis.spark.io.file.FileAttributeStore import FileAttributeStore
from nose import tools

@tools.istest
class FileAttributeStoreSpec(_AttributeStoreSpec):
    def __init__(self):
        _AttributeStoreSpec.__init__(self)
        self._store = None

    @property
    @tools.nottest
    def attributeStore(self):
        if self._store:
            return self._store
        self._store = FileAttributeStore(self.outputLocalPath)
        return self._store
