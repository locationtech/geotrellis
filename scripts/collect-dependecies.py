import os, re
import subprocess

projects = ["shapefile",
            "raster-testkit",
            "vector-testkit",
            "spark-testkit"]

org_flags = { "org.apache.accumulo": "accumulo",
              "org.geotools": "geotools",
              "com.typesafe.akka": "akka" }

ignore = [ ("org.locationtech.geotrellis", None, None) ]

if __name__ == "__main__":

    deps = set([])
    lines = []

    org_flag_indent = -1
    curr_org_flag = ""

    for project in projects:
        print "Checking %s..." % project
        output = subprocess.check_output(['./sbt "project %s" dependencyGraph' % (project)], shell=True)

        for line in output.split('\n'):
            if not 'evicted' in line:
                m = re.search(r"""(.*)\+-([^:]+):([^:]+):([\d.]+)""", line)
                if m:
                    lines.append(line)

                    org = m.group(2)
                    name = m.group(3)
                    version = m.group(4)
                    this_indent = m.group(1).count('|')

                    if name == "kryo":
                        print "KRYO %s %s %s" % (org, name, version)

                    if name == "protobuf-java":
                        print "PROTOBUF %s %s %s" % (org, name, version)

                    if name == "jai_core":
                        print "JAI %s %s %s" % (org, name, version)

                    org_flag = ""
                    if org_flag_indent != -1:
                        if org_flag_indent < this_indent:
                            org_flag = curr_org_flag
                        else:
                            curr_org_flag = ""
                            org_flag_indent = -1

                    if org_flag == "":
                        if org in org_flags:
                            org_flag = org_flags[org]
                            curr_org_flag = org_flag
                            org_flag_indent = this_indent

                    dep = (m.group(2), m.group(3), m.group(4), org_flag)
                    if org_flag:
                        print "%s %s" % (' ' * this_indent, str(dep))

                    deps.add(dep)

    s = '"org","name","version","flag"\n'

    for dep in sorted(deps):
        s += '"%s","%s","%s","%s"\n' % dep

    open('dependency-list.csv', 'w').write(s)
    open('dependency-list.txt', 'w').write('\n'.join(lines))
