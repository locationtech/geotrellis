# -*- coding: utf-8 -*-
"""
Defines a class for getting information on operations.
"""

import os,re
from lxml import html

def error(msg):
    print "Error: %s" % msg
    exit(1)

class Operation:
    def __init__(self,name,path,package):
        self.name = name
        self.path = path
        self.package = package
        self.description = None

    def __eq__(self,other):
        return self.name == other.name and self.package == other.package

    def __hash__(self):
        return hash(hash(self.name)+hash(self.package))

    def scaladoc_url(self):
        return 'http://geotrellis.github.com/api.doc/latest/api/#%s.%s' % (self.package,self.name)
    
class OperationsConstructor:
    def __init__(self,src_dir,api_dir):
        self.src_dir = src_dir
        self.api_dir = api_dir
    
    def check_file(self,res,fs):
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

    def get_operations(self):
        scala_files = []
        for (root,folders,files) in os.walk(self.src_dir):
            for f in filter(lambda x: x.endswith('.scala'), files):
                if not '#' in f:
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
            op_classes = set(self.check_file(res, scala_files)).union(op_classes)
            i += 1
            
        if i == 100:
            return (0,None)
            
        #order
        def comparer(op1, op2):
            if not op1.package == op2.package:
                if op1.package < op2.package:
                    -1
                else:
                    1
            if op1.name < op2.name:
                return -1 
            else:
                return 1
        op_classes = sorted(op_classes,cmp=comparer)
            
        # Get all doc snippets
        for op in op_classes:
            path = os.path.join(self.api_dir,'%s/%s.html' % (op.package.replace('.','/'),op.name))
            if not os.path.exists(path):
                continue
            root = html.parse(path).getroot()
            comment = root.get_element_by_id("comment")
            l = [comment.find_class('comment cmt'),comment.find_class('attributes block')]
            items = [item for sublist in l for item in sublist]
            map(lambda x: comment.remove(x), set(comment) - set(items))
            op.description = html.tostring(comment).strip().replace('\n\n','\n')
    
        return (i,sorted(op_classes,key=lambda x: x.package))
