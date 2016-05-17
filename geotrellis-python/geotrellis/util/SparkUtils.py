class SparkUtils(object):
    @staticmethod
    def hadoopConfiguration(jvm):
        jvm.org.apache.hadoop.conf.Configuration.addDefaultResource("core-site.xml")
        jvm.org.apache.hadoop.conf.Configuration.addDefaultResource("mapred-site.xml")
        jvm.org.apache.hadoop.conf.Configuration.addDefaultResource("hdfs-site.xml")
        return jvm.org.apache.hadoop.conf.Configuration()
