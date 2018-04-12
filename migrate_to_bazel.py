#!/bin/python
import glob, os, os.path
from subprocess import call

code_root = "core/src/main/java"

code_files = glob.iglob(code_root + "/**/*.java", recursive=True)
code_dirs = set(os.path.dirname(f) for f in code_files)

for code_dir in code_dirs:
  with open(code_dir + "/BUILD", 'w') as f:
    f.write("package(default_visibility = ['//:__subpackages__'])\n" +
            "java_library(name = '{}', srcs = glob(['*.java']))".format(os.path.basename(code_dir)))

cmd = [os.path.expanduser("~/bin/jadep"), '-content_roots='+code_root] + ['//'+x for x in code_dirs]
print (cmd)
call(cmd)

test_root = "core/src/test/java"
test_files = glob.glob(test_root + "/**/*.java", recursive=True)
test_dirs = set(os.path.dirname(f) for f in test_files)

cmd = [os.path.expanduser("~/bin/jadep"), '-content_roots=' + ','.join([code_root, test_root])] + test_files
print (cmd)
call(cmd)


# Replace java_library with java_test in test BUILD files.
for test_dir in test_dirs:
  f = test_dir + "/BUILD"
  call(['sed', '-i', "", 's/java_library/java_test/', f])
