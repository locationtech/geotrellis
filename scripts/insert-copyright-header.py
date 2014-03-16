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

import os, argparse, sys, re

license = """Copyright (c) 2014 %s.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""

max_width = max(map(lambda x: len(x), license.split('\n')))

def c_style_comment(text):
  commented = [ ]
  commented.append("/**" + ('*' * max_width))
  for line in text.split("\n"):
    commented.append(" * " + line)
  commented.append(" **" + ('*' * max_width) + "/")
  return '\n'.join(commented)

def python_comment(text):
  commented = [ ]
  for line in text.split("\n"):
    commented.append("# " + line)
  return '\n'.join(commented) + '\n'


re_c = re.compile(r'(\/\*\*\*.*?Copyright \(c\).*\*\*\*\/)', re.MULTILINE | re.DOTALL )
re_py = re.compile(r'((#.*?Copyright \(c\).*(?:\n|\r\n?))(#.*?(\n|\r\n?))*)', re.MULTILINE)

# Key: Regex for full path.
# Value: Tuple of (comment regex, comment function, comment regex, copyright company)
config = {
  r"\.\/spark.*?\.scala$" : (re_c, c_style_comment, "DigitalGlobe"),
  r".*\.scala$" : (re_c, c_style_comment, "Azavea"),
  r".*\.py$" : (re_py, python_comment, "Azavea")
}

re_c = re.compile(r'^((\/\*\*\*.*?(?:\n|\r\n?))(\*.*?Copyright \(c\).*?(?:\n|\r\n?))(\*.*?(?:\n|\r\n?))(.*?\*\*\*\/(?:\n|\r\n?)))', re.MULTILINE)
re_py = re.compile(r'^((#.*?Copyright \(c\).*(?:\n|\r\n?))(#.*(?:\n|\r\n?))*)', re.MULTILINE)
  
def insert_header(path, pattern, comment):
  f = open(path, "r")
  text = f.read()
  f.close()
  
  (sub_text, n) = pattern.subn(comment, text, count = 1)
  if n == 0:
    sub_text = comment + "\n\n" + text
    print "\tadded"
  else:
    print "\tupdated"
    
  f = open(path, "w")
  f.write(sub_text)
  f.close


def handle(dir, file):
  path = os.path.join(dir, file)

  for (pattern, (re_matcher, f_comment, owner)) in config.iteritems():
    m = re.match(pattern, path)
    if m:
      print(path)
      license_comment = f_comment(license % owner)
      insert_header(path, re_matcher, license_comment)
      break
  
def main():
  #Start walking from CWD and apply rules defined in config
  for root, dirs, files in os.walk("."):
    for f in files:
      handle(root, f)

if __name__ == "__main__":
  main()
