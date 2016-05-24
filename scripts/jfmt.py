#!/usr/bin/env python2.7

from __future__ import print_function
import sys, os


# Instruction:
# Script can be called from python interpreter
# or aliased and used as any other UNIX tools,
# like 'cd' or 'cat'.
#
# In order to use this script like 'UNIX tool':
# $ chmod +x jfmt.py
# And add alias to your profile or rc file:
# alias jfmt="~/path/to/jfmt.py"
#
#
# Behaviour:
# When script called in directory without arguments,
# or on directory it will format every .java file
# in current (or provided) directory AND child directories.
# 
# When script called with argument which is path to .java
# file, only this file will be formated.


# Get script location path in order to find
# a real path of the google-java-format.
def origin_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))

# Constants:
# Current version name
GJF = "google-java-format-1.0-all-deps.jar"
# Command which will be executed by the os.system()
COMMAND = "java -jar " + origin_path() + "/" + GJF
# Verbose output.
DEBUG = False

def format_files(p):
    '''
    Format files in `p` directory.
    '''
    for (dirpath, dnames, fnames) in os.walk(p):
        for fname in fnames:
            if fname.endswith('.java'):
                fpath = dirpath + '/' + fname
                c = COMMAND + " --replace " + fpath
                os.system(c)
                if DEBUG:
                    print("Path: ", dirpath)
                    print("Name: ", fname)

def format_file(p):
    '''
    Format the file.
    '''
    c = COMMAND + " --replace " + p
    os.system(c)

def parse_argv():
    if len(sys.argv) == 2:
        p = os.path.abspath(sys.argv[1])
        if os.path.isfile(p):
            format_file(p)
        if os.path.isdir(p):
            format_files(p)
    if len(sys.argv) == 1:
        p = os.getcwd()
        format_files(p)

def print_help(reason):
    if reason == "jar":
        print("ERROR: No " + GJF + " found")
        print("\nIn order to use 'jfmt' you need to put " +
              "'google-java-format-1.0-all-deps.jar' into the folder " +
              "with 'jfmt' script.\n")

def check_source():
    '''
    Check is google-java-format is present or not.
    '''
    o = origin_path()
    p = o + "/" + GJF
    if os.path.isfile(p):
        return True
    else:
        return False

def main():
    if check_source():
        print("Processing...")
        parse_argv()
        print("Done.")
    else:
        print_help("jar")

if __name__ == '__main__':
    main()