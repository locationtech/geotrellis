
class HdfsUtils(object):
    @staticmethod
    def getTempDir(conf):
        return conf.get("hadoop.tmp.dir", "/tmp")
