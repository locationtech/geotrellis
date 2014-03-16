#!/usr/bin/env python

# Copyright (c) 2014 Azavea.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


from operation import *

def error(msg):
    print "ERROR: %s" % msg
    exit(1)

def run():
    oc = OperationsConstructor('/home/rob/proj/gt/geotrellis/src/main/scala/geotrellis',
                               '/home/rob/proj/gt/geotrellis/target/scala-2.10/api')
    (i,op_classes) = oc.get_operations()
    if not op_classes:
        error("Could not gather operations.")

    txt = """---
layout: oplist
title: Operations List

tutorial: operations
num: 10
---

<div class="filterbar">
  <span class="title">Look up a term</span>
  <input class="field" id="filter" type="text" />
  <span id="filter-count"></span>
</div>

"""
    for op in op_classes:
        #txt += '* #### %s\n%s\n\n' % (op.name,op.description,) 
        txt += '<tr><td><code><a href="%s" target="_blank">%s.%s</a></code></td><td>%s</td></tr>\n' % (op.scaladoc_url(),
                                                                                                      op.package.replace('geotrellis.',''),
                                                                                                      op.name,
                                                                                                      op.description)
    open('operations.md','w').write(txt)

    txt = '"Operation","Package","Path","Supports Doubles?"\n'
    for op in op_classes:
        txt += '"%s","%s","%s",""\n' % (op.name,op.package,op.path)
    open('operations.csv','w').write(txt)
        
    print "After %d runs got %d operations." % (i,len(op_classes))

if __name__ == '__main__':
    run()
