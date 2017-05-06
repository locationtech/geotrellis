import os, sys, re, argparse, csv
import subprocess
from subprocess import Popen, PIPE, STDOUT, call

class DepSet(object):
    def __init__(self, deps_map, filter_func=lambda x: True):
        self.deps_map = {}
        for k in deps_map:
            if filter_func(deps_map[k]):
                self.deps_map[k] = deps_map[k]
        self.deps = set(self.deps_map.keys())

        self.versions = {}
        self.dep_tag_map = {}
        for o, n, v in self.deps:
            self.versions[(o, n)] = v
            self.dep_tag_map[(o, n)] = self.deps_map[(o, n, v)]

    def version(self, org, name):
        return self.versions[(org, name)]

    def tags(self, org, name):
        return self.dep_tag_map[(org, name)]

    def deps_only(self):
        return set(map(lambda d: (d[0], d[1]), self.deps))

def source_link(org, name, version):
    if not org.startswith("org.geotools"):
        s = (org.replace('.', '/'), name, version, name, version)
        return "http://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s-sources.jar" % s
    else:
        s = (org.replace('.', '/'), name, version, name, version)
        return "http://download.osgeo.org/webdav/%s/%s/%s/%s-%s-sources.jar" % s


published_projects = ['accumulo',
                      'cassandra',
                      'geomesa',
                      'geotools',
                      'geowave',
                      'hbase',
                      'macros',
                      'proj4',
                      'raster',
                      'raster-testkit',
                      's3',
                      's3-testkit',
                      'shapefile',
                      'slick',
                      'spark',
                      'spark-etl',
                      'spark-testkit',
                      'util',
                      'vector',
                      'vector-testkit',
                      'vectortile']

org_flags = { "org.apache.accumulo": "from:accumulo",
              "org.geotools": "from:geotools",
              "com.typesafe.akka": "from:akka",
              "mil.nga.giat": "from:geowave" }

ignore_orgs = [ "org.locationtech",
                "org.eclipse",
                "mil.nga.giat",
                "it.geosolutions" # CQ #10604
]

def gather_dependencies(projects):
    """
    Gathers dependencies, and returns in a dict where the key is the triple (org, name, version),
    and the value is any tags associated with that dependency
    """
    deps = { }

    org_flag_indent = -1
    curr_org_flag = ""
    ignore_org_indent = -1
    curr_ignore_org = ""

    for project in projects:
        print("Checking %s..." % project)
        cmd = ['sbt', '-no-colors', '%s/dependencyTree' % project]
        print(cmd)
        output = ""
        with Popen(cmd, stdout=PIPE, stderr=STDOUT, bufsize=1, universal_newlines=True) as p:
            for l in p.stdout:
                line = l.strip()
                print(line)

                if not 'evicted' in line:
                    m = re.search(r"""(.*)\+-([^:]+):([^:]+):([\d.]+)""", line)
                    if m:
                        org = m.group(2)
                        name = m.group(3)
                        version = m.group(4)
                        this_indent = m.group(1).count('|')

                        if name == "kryo":
                            print("KRYO %s %s %s" % (org, name, version))

                        if name == "protobuf-java":
                            print("PROTOBUF %s %s %s" % (org, name, version))

                        if name == "jai_core":
                            print("JAI %s %s %s" % (org, name, version))

                        ## Check if we should be ignoring these depenedencies
                        ignore_org = None


                        # If we're already under an ignore dep, and we haven't moved
                        # the indent back beyond it yet, then ignore it.
                        if ignore_org_indent != -1:
                            if ignore_org_indent < this_indent:
                                ignore_org = curr_ignore_org
                            else:
                                curr_ignore_org = None
                                ignore_org_indent = -1

                        # If the org flag wasn't set, our current org should be flagged, set
                        # up the org flag.
                        if not ignore_org:
                            for o in ignore_orgs:
                                if o in org:
                                    ignore_org = org
                                    curr_ignore_org = ignore_org
                                    ignore_org_indent = this_indent

                        if not ignore_org:

                            ## Set org flag

                            org_flag = ""

                            # If we're already under an org flag, and we haven't moved
                            # the indent back beyond it yet, then that's our org flag.
                            if org_flag_indent != -1:
                                if org_flag_indent < this_indent:
                                    org_flag = curr_org_flag
                                else:
                                    curr_org_flag = ""
                                    org_flag_indent = -1

                            # If the org flag wasn't set, our current org should be flagged, set
                            # up the org flag.
                            if org_flag == "":
                                if org in org_flags:
                                    org_flag = org_flags[org]
                                    curr_org_flag = org_flag
                                    org_flag_indent = this_indent



                            dep = (org, name, version)

                            if dep in deps:
                                if org_flag:
                                    deps[dep].add(org_flag)
                            else:
                                if org_flag:
                                    deps[dep] = set([org_flag])
                                else:
                                    deps[dep] = set([])
    return deps

def write_dependencies(deps, version, output):
    s = '"org","name","version","tags"\n'

    for dep in sorted(deps.keys()):
        r = (dep[0], dep[1], dep[2], ",".join(deps[dep]))
        s += '"%s","%s","%s","%s"\n' % r

    open(output, 'w').write(s)

# def write_version_dependencies(old_version, new_version):
#     current_branch = subprocess.check_output(['git status -sb'], shell=True).decode('utf8').split('\n')[0][3:]

#     # Gather old dependencies
#     subprocess.call('git checkout ' + old_version, shell=True)
#     old_deps = gather_dependencies(version_projects[old_version])
#     write_dependencies(old_deps, old_version)

#     # Gather new dependencies
#     subprocess.call('git checkout ' + new_version, shell=True)
#     new_deps = gather_dependencies(version_projects[new_version])
#     write_dependencies(new_deps, new_version)

#     subprocess.call('git checkout ' + current_branch, shell=True)

#     print("Done.")

def run_write(args):
    version = args.version
    output = "dependencies-%s.csv" % version
    if args.output:
        output = args.output

    current_branch = subprocess.check_output(['git status -sb'], shell=True).decode('utf8').split('\n')[0][3:]
    if subprocess.call('git checkout ' + version, shell=True) == 0:
        try:
            deps = gather_dependencies(published_projects)
            write_dependencies(deps, version, output)
        finally:
            subprocess.call('git checkout ' + current_branch, shell=True)
    else:
        print("git checkout errored!")

def read_deps_file(path):
    deps = {}
    with open(path, 'r') as csvfile:
        for row in csv.DictReader(csvfile):
            org = row['org']
            name = row['name']
            version = row['version']
            tags = row['tags'].split(',')
            deps[(org, name, version)] = tags
    return deps

def read_diff_file(path):
    deps = {}
    with open(path, 'r') as csvfile:
        for row in csv.DictReader(csvfile):
            org = row['org']
            name = row['name']
            old_version = row['old_version']
            new_version = row['new_version']
            tags = row['tags'].split(',')
            sources_link = row['sources_link']
            deps[(org, name, new_version)] = (sources_link, tags, old_version)
    return deps

def run_diff(args):
    old = DepSet(read_deps_file(args.old_version))
    new = DepSet(read_deps_file(args.new_version))

    output = "DIFF-%s-%s.csv" % (args.old_version, args.new_version)
    if args.output:
        output = args.output

    s = '"org","name","old_version","new_version","tags","sources_link"\n'

    print("NEW:")
    for org, name in new.deps_only() - old.deps_only():
        v = new.version(org, name)
        tags = ','.join(new.tags(org, name))
        sl = source_link(org, name, v)
        r = (org, name, "None",  v, tags, sl)
        s += '"%s","%s","%s","%s","%s","%s"\n' % r
        print("\t%s : %s : %s\t\t%s" % (org, name, v, tags))

    print("UPGRADED:")
    upgraded = []
    for org, name in new.deps_only().intersection(old.deps_only()):
        new_version = new.version(org, name)
        old_version = old.version(org, name)
        tags = ','.join(new.tags(org, name))
        sl = source_link(org, name, new_version)
        if new_version != old_version:
            print("\t%s : %s :  %s -> %s\t\t%s" % (org, name, old_version, new_version, tags))
            r = (org, name, old_version, new_version, tags, sl)
            s += '"%s","%s","%s","%s","%s","%s"\n' % r

    open(output, 'w').write(s)

def run_download(args):
    output = "cq-sources/"
    if args.output:
        output = args.output

    diffs = read_diff_file(args.diff_file)
    for org, name, version in diffs:
        (link, _, _) = diffs[(org, name, version)]

        d = os.path.join(output, org)
        if not os.path.exists(d):
            os.makedirs(d)
        p = os.path.join(d, "%s-%s-sources.zip" % (name, version))
        call("wget -O %s %s" % (p, link), shell=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Describe dependencies of this project.')
    parser.set_defaults(func=lambda x: parser.print_help())
    subparsers = parser.add_subparsers(help='sub-command help')

    parser_write = subparsers.add_parser('write', help='Write out dependencies for a version.')
    parser_write.add_argument('--output', '-o', metavar='OUTPUT')
    parser_write.add_argument('version', metavar='VERSION', help='Branch name, tag, or commit to write dependencies')
    parser_write.set_defaults(func=run_write)

    parser_diff = subparsers.add_parser('diff', help='Diff dependencies between two versions.')
    parser_diff.add_argument('--output', '-o', metavar='OUTPUT')
    parser_diff.add_argument('old_version', metavar='OLD_DEPENDENCIES', help='Path to CSV of old dependencies, written through the "write" subcommand')
    parser_diff.add_argument('new_version', metavar='NEW_DEPENDENCIES', help='Path to CSV of new dependencies, written through the "write" subcommand')
    parser_diff.set_defaults(func=run_diff)

    parser_download = subparsers.add_parser('download', help='Download source jars as source zips.')
    parser_download.add_argument('--output', '-o', metavar='OUTPUT_DIRECTORY')
    parser_download.add_argument('diff_file', metavar='DIFF_FILE', help='Path to CSV of dependency diff, from "diff" command.')
    parser_download.set_defaults(func=run_download)


    args = parser.parse_args()
    args.func(args)
