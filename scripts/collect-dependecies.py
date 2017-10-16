import os, sys, re, argparse, csv, shutil, json
import subprocess
from subprocess import Popen, PIPE, STDOUT, call
import networkx as nx
from networkx.readwrite import json_graph

scala_versions = ['2.11', '2.12']
GT_VERSION = "1.1"

class Dependency(object):
    def __init__(self, org, name, version, tags=[]):
        self.org = org.strip()
        self.name = name.strip()
        self.version = version.strip()
        self.tags = tags

    def __hash__(self):
        return hash((self.org, self.name, self.version))

    def __repr__(self):
        return "%s:%s:%s" % (self.org, self.name, self.version)

    def __eq__(self, other):
        if isinstance(other, Dependency):
            return (self.org == other.org) and \
                   (self.name == other.name) and \
                   (self.version == other.version)
        else:
            return false

    def __neq__(self, other):
        return not self.__eq__(other)

    def source_link(self):
        (org, name, version) = (self.org, self.name, self.version)
        if not org.startswith("org.geotools"):
            s = (org.replace('.', '/'), name, version, name, version)
            return "http://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s-sources.jar" % s
        else:
            s = (org.replace('.', '/'), name, version, name, version)
            return "http://download.osgeo.org/webdav/geotools/%s/%s/%s/%s-%s-sources.jar" % s
    @classmethod
    def from_string(cls, s):
        l = s.split(':')
        return cls(l[0], l[1], l[2])

class DependencySet(object):
    def __init__(self, dep_names):
        self.deps = []
        for l in map(lambda x: x.split(':'), dep_names):
            self.deps.append(Dependency(l[0], l[1], l[2]))

        self.versions = {}
        self.dep_tag_map = {}
        for dep in self.deps:
            self.versions[(dep.org, dep.name)] = dep.version

    def version(self, org, name):
        return self.versions[(org, name)]


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
        return "http://download.osgeo.org/webdav/geotools/%s/%s/%s/%s-%s-sources.jar" % s


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

# If the org equals the key, place in the corresponding group
orgs_to_dl_groups = { "org.geotools" : "geotools",
                      "org.geotools.ogc" : "geotools",
                      "org.geotools.xsd" : "geotools",
                      "org.apache.hbase" : "hbase",
                      "org.scalaz" : "scalaz",
                      "com.amazonaws" : "aws-java-sdk"}

# If the name starts with the key, place in the corresponding group
names_to_dl_groups = { "monocle" : "monocle",
                       "slick-pg" : "slick-pg",
                       "fastparse" : "fastparse",
                       "scalactic" : "scalatest",
                       "scalatest" : "scalatest" }


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
                    m = re.search(r"""(.*)\+-([^:]+):([^:]+):([\d\w-.]+)""", line)
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

def generate_dependency_graph_old(projects):
    G = nx.DiGraph()

    for project in projects:
    # for project in ['raster']:
        dep_stack = []
        last_indent = 0

        print("Checking %s..." % project)
        cmd = ['sbt', '-no-colors', '%s/dependencyTree' % project]
        print(cmd)
        output = ""

        def add_edge(d):
            if dep_stack:
                G.add_edge(str(dep_stack[-1]), str(d))
                print("EDGE %s -> %s" % (str(dep_stack[-1]), str(d)))
            else:
                G.add_node(str(d))

        with Popen(cmd, stdout=PIPE, stderr=STDOUT, bufsize=1, universal_newlines=True) as p:
            for l in p.stdout:
                line = l.strip()
                print(line)

                if not 'evicted' in line:
                    mr = re.search(r"""\[info\] (org\.locationtech\.geotrellis):([^:]+):([-\d\w.]+)""", line)
                    m = re.search(r"""\[info\](.*)\+-([^:]+):([^:]+):([-\d\w.]+)""", line)
                    if mr:
                        org = mr.group(1)
                        name = mr.group(2)
                        version = mr.group(3)
                        this_indent = 0
                    elif m:
                        org = m.group(2)
                        name = m.group(3)
                        version = m.group(4)
                        this_indent = len(m.group(1)) / 2

                    if m or mr:
                        dep = Dependency(org, name, version)

                        if name == "machinist_2.11":
                            print("THIS INDENT: %d  LAST INDENT: %d" % (this_indent, last_indent))

                        if this_indent > last_indent:
                            add_edge(dep)
                            dep_stack.append(dep)
                        elif this_indent == last_indent:
                            dep_stack = dep_stack[:-1]
                            add_edge(dep)
                            dep_stack.append(dep)
                        elif this_indent == 0:
                            add_edge(dep)
                            dep_stack.append(dep)
                        else:
                            dep_stack = dep_stack[:-1]
                            d = int(this_indent - last_indent) # negative
                            dep_stack = dep_stack[:d]
                            add_edge(dep)
                            dep_stack.append(dep)

                        last_indent = this_indent
    return G

def generate_dependency_graph(projects):
    G = nx.DiGraph()

    for project in projects:
    # for project in ['raster']:
        dep_stack = []
        last_indent = 0

        print("Checking %s..." % project)
        cmd = ['sbt', '-no-colors', '%s/dependencyDot' % project]
        print(cmd)
        output = ""

        with Popen(cmd, stdout=PIPE, stderr=STDOUT, bufsize=1, universal_newlines=True) as p:
            for l in p.stdout:
                line = l.strip()
                print(line)

                if not 'evicted' in line:
                    m = re.search(r"""Wrote dependency graph to '([^']+)'""", line)

                    if m:
                        path = m.group(1)
                        H = nx.DiGraph(nx.drawing.nx_pydot.read_dot(path))
                        G = nx.compose(G, H)

    return G

def run_graphml_export(args):
    """
    Generates the GraphML file
    """
    output_path = args.output
    if not output_path:
        output_path = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'dependencies.graphml')
    print("Using output %s" % output_path)

    projects = published_projects
    if args.projects:
        projects = args.projects.split(',')

    G = generate_dependency_graph(projects)

    nx.write_gml(G, output_path)
    # data = json_graph.node_link_data(G)
    # open("scripts/dependencies.json", 'w').write(json.dumps(data))


def write_dependencies(deps, version, output):
    s = '"org","name","version","tags"\n'

    for dep in sorted(deps.keys()):
        r = (dep[0], dep[1], dep[2], ",".join(deps[dep]))
        s += '"%s","%s","%s","%s"\n' % r

    open(output, 'w').write(s)

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

    output = "DIFF-%s-%s.csv" % (args.old_version.replace('.csv',''), args.new_version.replace('.csv',''))
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
    errored = []
    for org, name, version in diffs:
        group = None
        if org in orgs_to_dl_groups:
            group = orgs_to_dl_groups[org]
        for k in names_to_dl_groups:
            if name.startswith(k):
                group = names_to_dl_groups[k]

        (link, _, _) = diffs[(org, name, version)]

        if group:
            d = os.path.join(output, group)
        else:
            n = name
            for v in scala_versions:
                n = n.replace('_%s' % v, '')
            d = os.path.join(output, n)
        if not os.path.exists(d):
            os.makedirs(d)
        p = os.path.join(d, "%s-%s-%s-sources.zip" % (org, name, version))
        cmd = "wget -O %s %s" % (p, link)
        # print(cmd)
        if call(cmd, shell=True) > 0:
            print("ERRORED!")
            errored.append((org, name, version))

    if errored:
        print("THESE DEPENDENCIES DID NOT DOWNLOAD:")
        for org, name, version in errored:
            print("\t%s : %s : %s" % (org, name, version))

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

    parser_graphml = subparsers.add_parser('graphml', help='Generate GraphML for dependencies.')
    parser_graphml.add_argument('--output', '-o', metavar='OUTPUT')
    parser_graphml.add_argument('--projects', '-p', metavar='PROJECTS', help='Comma seperated list of subprojects to run.')
    parser_graphml.set_defaults(func=run_graphml_export)


    args = parser.parse_args()
    args.func(args)
