from __future__ import absolute_import

class SerializationException(Exception):
    def __init__(self, msg, cause = None, field_names = []):
        self.msg = msg
        self.cause = cause

class DeserializationException(Exception):
    def __init__(self, msg, cause = None, field_names = []):
        self.msg = msg
        self.cause = cause
