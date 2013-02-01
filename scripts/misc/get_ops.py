import os,re

class Operation:
    def __init__(self,name,path,package):
        self.name = name
        self.path = path
        self.package = package

    def __eq__(self,other):
        return self.name == other.name and self.package == other.package

    def __hash__(self):
        return hash(hash(self.name)+hash(self.package))

    def scaladoc_url(self):
        return 'http://geotrellis.github.com/latest/api/#%s.%s' % (self.package,self.name)

def check(res,fs):
    result = []
    for f in fs:
        txt = open(f).read()
        package = re.search(r'package\s+([\w\.]+)',txt).group(1)
        for m in  re.finditer(res,txt):
            # Discount private, protected, or commented
            prefix = m.group(1)
            if 'private' in prefix:
                continue
            if 'protected' in prefix:
                continue
            if '//' in prefix:
                continue
            if '*' in prefix:
                continue

            print m.group(1) + '   ' + f + '     ' + m.group("name")
            
            if not 'private' in m.group(1) and not 'protected' in m.group(1):
                op = Operation(m.group("name"),f,package)
                result.append(op)
    return result

def run():
    d = '/home/rob/proj/gt/geotrellis/src/main/scala/geotrellis'
    scala_files = []
    for (root,folders,files) in os.walk(d):
        for f in filter(lambda x: x.endswith('.scala'), files):
            scala_files.append(os.path.join(root,f))

    root_classes = ['Operation',
                  'Op\[',
                  'Op0',
                  'Op1',
                  'Op2',
                  'Op3',
                  'Op4',
                  'Op5',
                  'Op6',]

    i = 0
    op_classes = []
    last_opclasses = []
    while i < 100 and (len(op_classes) != len(last_opclasses) or i == 0):
        last_opclasses = list(op_classes)
        opstr = reduce(lambda x,y: x + '|' + y,
                       root_classes + map(lambda x: x.name, op_classes))
        res = r'(?sm)([^\n]*) ?(class|trait|object)\s+(?P<name>[^\s(\[\]]+)[^{*]*?extends\s+(%s)' % opstr
        op_classes = set(check(res, scala_files)).union(op_classes)
        i += 1

    op_classes = sorted(op_classes,key=lambda x: x.package)

    if i == 100:
        print "ERROR: Overflow."
    else:
        txt = ""
        for op in op_classes:
            txt += '* #### %s\nDescription goes here.\n[See API documentation for %s](%s)\n' % (op.name,op.name,op.scaladoc_url()) 
        open('operations.md','w').write(txt)

        txt = '"Operation","Package","Path","Supports Doubles?"\n'
        for op in op_classes:
            txt += '"%s","%s","%s",""\n' % (op.name,op.package,op.path)
        open('operations.csv','w').write(txt)
            
        print "After %d runs got %d operations." % (i,len(op_classes))

if __name__ == '__main__':
    run()
