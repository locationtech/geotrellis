from __future__ import absolute_import
import os
import os.path

class Filesystem(object):

    @staticmethod
    def ensureDirectory(path):
        if os.path.isfile(path):
            raise Exception("{path} exists and is not a directory".format(path = path))
        if not os.path.isdir(path):
            os.makedirs(path)
        return path

    @staticmethod
    def writeBytes(path, contents):
        with open(path, "wb") as f:
            f.write(contents)
