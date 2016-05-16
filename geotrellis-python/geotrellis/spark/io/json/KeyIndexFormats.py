from __future__ import absolute_import
from geotrellis.python.util.utils import JSONFormat, fullname, find
from geotrellis.python.spray.json.package_scala import SerializationException, DeserializationException
from geotrellis.spark.io.json.KeyFormats import KeyBoundsFormat, SpatialKeyFormat, SpaceTimeKeyFormat
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.SpaceTimeKey import SpaceTimeKey

class KeyIndexFormatEntry(object):
    def __init__(self, K, T, typeName):
        self.typeName = typeName
        self.keyClassTag = fullname(K)
        self.jsonFormat = T.implicits["format"]()
        self.indexClassTag = fullname(T)

    def __eq__(self, other):
        if not isinstance(other, KeyIndexFormatEntry):
            return False
        return (self.typeName == other.typeName and
                self.keyClassTag == other.keyClassTag and
                self.indexClassTag == other.indexClassTag
                # and self.jsonFormat == other.jsonFormat # TODO jsonFormat.__eq__ if needed
                )

    def __hash__(self):
        return hash((self.typeName, self.keyClassTag, self.indexClassTag
            #, self.jsonFormat # TODO jsonFormat.__hash__ if needed
            ))

class KeyIndexJsonFormat(JSONFormat):
    def __init__(self, entries):
        self.entries = entries
    def to_dict(self, obj):
        objectTypeName = fullname(type(obj))
        found = find(self.entries, 
                lambda e: e.indexClassTag == objectTypeName)
        if found is None:
            raise SerializationException(
                    "Cannot serialize this key index with" +
                    " this KeyIndexJsonFormat: {obj}".format(obj=obj))
        return found.jsonFormat.to_dict(obj)

    def from_dict(self, dct):
        fields = self.get_fields(dct, 'type', 'properties')
        if not fields:
            raise DeserializationException(
                    "Expected KeyIndex, got {dct}".format(dct=dct))
        typeName, properties = fields
        found = find(self.entries, lambda e: e.typeName == typeName)
        if found is None:
            raise DeserializationException("Cannot deserialize key index with type {tp} in json {dct}".format(tp=typeName, dct=dct))
        return found.jsonFormat.from_dict(dct)

class KeyIndexRegistry(object):
    def __init__(self):
        self._entries = []
    def register(self, entry):
        self._entries.append(entry)
    @property
    def entries(self):
        return self._entries[:]

_REG_SETTING_NAME = "geotrellis.python.spark.io.index.registrator"

def _generate_registry():
    entryRegistry = KeyIndexRegistry()
    from geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex import ZSpatialKeyIndex
    from geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex import ZSpaceTimeKeyIndex
    from geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex import RowMajorSpatialKeyIndex
    entryRegistry.register(KeyIndexFormatEntry(SpatialKey, ZSpatialKeyIndex, ZSpatialKeyIndexFormat.TYPE_NAME))
    entryRegistry.register(KeyIndexFormatEntry(SpatialKey, RowMajorSpatialKeyIndex, RowMajorSpatialKeyIndexFormat.TYPE_NAME))
    entryRegistry.register(KeyIndexFormatEntry(SpaceTimeKey, ZSpaceTimeKeyIndex, ZSpaceTimeKeyIndexFormat.TYPE_NAME))
    # TODO user defined registrator
    tuples = map(lambda e: (e.keyClassTag, e), entryRegistry.entries)
    from collections import defaultdict
    res = defaultdict(list)
    for k, e in tuples: res[k].append(e)
    return res

class _KeyIndexJsonFormatFactory(object):
    _registry = None
    @property
    def registry(self):
        if self._registry is not None:
            return self._registry
        registry = _generate_registry()
        self._registry = registry
        return registry

    def getKeyIndexJsonFormat(self, K):
        nameToFind = fullname(K)
        if not self.registry.has_key(nameToFind):
            raise DeserializationException((
                    "Cannot deserialize key index for key type {keyType}. " + 
                    "You need to register this key type using the config item {regSetting}").format(keyType=nameToFind, regSetting=_REG_SETTING_NAME))
        return KeyIndexJsonFormat(self.registry[nameToFind])

KeyIndexJsonFormatFactory = _KeyIndexJsonFormatFactory()

class RowMajorSpatialKeyIndexFormat(JSONFormat):
    TYPE_NAME = "rowmajor"

    def to_dict(self, obj):
        kbFormat = KeyBoundsFormat(SpatialKeyFormat())
        kb = kbFormat.to_dict(obj.keyBounds)

        return {"type": self.TYPE_NAME,
                "properties": {"keyBounds": kb}}

    def from_dict(self, dct):
        fields = self.get_fields(dct, 'type', 'properties')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex type:" +
                    " RowMajorSpatialKeyIndex expected.")
        typename, props = fields
        if typename != self.TYPE_NAME:
            raise DeserializationException(
                    "Wrong KeyIndex type: {0} expected.".format(
                        self.TYPE_NAME))
        fields = self.get_fields(props, 'keyBounds')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex constructor arguments:" +
                    " RowMajorSpatialKeyIndex constructor arguments expected.")
        key_bounds_dict = fields[0]
        key_bounds = KeyBoundsFormat(SpatialKeyFormat()).from_dict(key_bounds_dict)
        from geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex import RowMajorSpatialKeyIndex
        return RowMajorSpatialKeyIndex(key_bounds)

class ZSpaceTimeKeyIndexFormat(JSONFormat):
    TYPE_NAME = "zorder"

    def to_dict(self, obj):
        kbFormat = KeyBoundsFormat(SpaceTimeKeyFormat())
        kb = kbFormat.to_dict(obj.keyBounds)

        return {"type": self.TYPE_NAME,
                "properties": {
                    "keyBounds": kb,
                    "temporalResolution": obj.temporalResolution}}

    def from_dict(self, dct):
        fields = self.get_fields(dct, 'type', 'properties')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex type:" +
                    " ZSpaceTimeKeyIndex expected.")
        typename, props = fields
        if typename != self.TYPE_NAME:
            raise DeserializationException(
                    "Wrong KeyIndex type: {0} expected.".format(
                        self.TYPE_NAME))
        fields = self.get_fields(props, 'keyBounds', 'temporalResolution')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex constructor arguments:" +
                    " ZSpaceTimeKeyIndex constructor arguments expected.")
        key_bounds_dict, temporalResolution = fields
        key_bounds = KeyBoundsFormat(SpaceTimeKeyFormat()).from_dict(key_bounds_dict)
        temporalResolution = long(temporalResolution) # TODO overflow like in java
        from geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex import ZSpaceTimeKeyIndex
        return ZSpaceTimeKeyIndex.byMilliseconds(key_bounds, temporalResolution)

class ZSpatialKeyIndexFormat(JSONFormat):
    TYPE_NAME = "zorder"

    def to_dict(self, obj):
        kbFormat = KeyBoundsFormat(SpatialKeyFormat())
        kb = kbFormat.to_dict(obj.keyBounds)

        return {"type": self.TYPE_NAME,
                "properties": {"keyBounds": kb}}

    def from_dict(self, dct):
        fields = self.get_fields(dct, 'type', 'properties')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex type:" +
                    " ZSpatialKeyIndex expected.")
        typename, props = fields
        if typename != self.TYPE_NAME:
            raise DeserializationException(
                    "Wrong KeyIndex type: {0} expected.".format(
                        self.TYPE_NAME))
        fields = self.get_fields(props, 'keyBounds')
        if not fields:
            raise DeserializationException(
                    "Wrong KeyIndex constructor arguments:" +
                    " ZSpatialKeyIndex constructor arguments expected.")
        key_bounds_dict = fields[0]
        key_bounds = KeyBoundsFormat(SpatialKeyFormat()).from_dict(key_bounds_dict)
        from geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex import ZSpatialKeyIndex
        return ZSpatialKeyIndex(key_bounds)
