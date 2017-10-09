#!/usr/bin/env python2.7

from __future__ import print_function
import sys, os
import argparse

# Purpose:
# Format all .java files in the directory, including child
# directories and their .java files, or just a single .java
# file.
#
# Instruction:
# Script can be called from python interpreter or aliased
# and used as any other UNIX tools, like 'cd' or 'cat'.
#
# In order to use this script like 'UNIX tool':
# $ chmod +x jfmt.py
# And add alias to your profile or rc file:
# alias jfmt="~/path/to/jfmt.py"
#
#
# Behaviour:
# When script called in directory without arguments, or on
# directory it will format every .java file in current
# (or provided) directory AND child directories.
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


def format_files(p, verbose):
    '''
    Format files in `p` directory.
    '''
    for (dirpath, dnames, fnames) in os.walk(p):
        for fname in fnames:
            if fname.endswith('.java'):
                fpath = dirpath + '/' + fname
                c = COMMAND + " --replace " + fpath
                os.system(c)
                if verbose:
                    print("Path: ", dirpath)
                    print("Name: ", fname)


def format_file(p, verbose):
    '''
    Format the file.
    '''
    if verbose:
        print("Format file: ", p)
    c = COMMAND + " --replace " + p
    os.system(c)


def parse_argv():
    parser = argparse.ArgumentParser(
        description='Format all .java files in the directory,' +
        ' including child directories and their .java files, ' +
        'or just a single .java file.')
    parser.add_argument('--verbose',
                        '-v',
                        action='store_true',
                        help='verbose flag')
    parser.add_argument('file',
                        nargs='?',
                        default='none',
                        help='path to .java file or folder')
    p = parser.parse_args()

    # Format .java files in current working directory.
    if len(sys.argv) == 1 or len(sys.argv) == 2 and p.verbose:
        path = os.getcwd()
        if p.verbose:
            print("Path: ", path)
            format_files(path, True)
        else:
            format_files(path, False)

    # Format .java files in proveded directory, or format .java
    # if proveded argument is a path to it.
    if p.file != 'none':
        if os.path.isfile(p.file):
            if p.verbose:
                format_file(p.file, True)
            else:
                format_file(p.file, False)
        if os.path.isdir(p.file):
            if p.verbose:
                print("Format dir: ", p.file)
                format_files(p.file, True)
            else:
                format_files(p.file, False)


def print_help(reason):
    if reason == "jar":
        print("ERROR: No " + GJF + " found")
        print("\nIn order to use 'jfmt' you need to put " + GJF +
              " into the folder " + "with 'jfmt' script.\n")


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
        parse_argv()
        print("Done")
    else:
        print_help("jar")


if __name__ == '__main__':
    main()