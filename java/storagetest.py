#!/usr/bin/env python
from dl import authClient as ac, queryClient as qc, storeClient as sc
import getpass
import os
import os.path
import sys
import datetime
import filecmp
import tempfile
import argparse

def timed_ls(dir, print_list=False):
    start = datetime.datetime.now()
    rawls = sc.ls (dir, format="long")
    if not rawls or rawls[-1] is not '\n':
        print ("{} {} {}: {}".format(datetime.datetime.now() - start, 0, dir, rawls))
        return []
    else:
        if print_list: print (rawls);
        flist = rawls.rstrip().split('\n')
        print ("{} {} {}".format(datetime.datetime.now() - start, len(flist), dir))
        return flist

def recursive_ls(dir, max_depth=None):
    flist = timed_ls(dir)
    for f in flist:
        name = f.split(' ')[-1]
        if f[0] == "d" and (max_depth is None or (dir.count('/') - 1 <= max_depth)):
            recursive_ls(dir + name, max_depth=max_depth)

def make_copies(file, dir, n_copies):
    for i in range(n_copies):
        f_name, f_ext = os.path.splitext(os.path.basename(file))
        n_name = "{}/{}_{:04}{}".format(dir, f_name, i, f_ext)
        print ("{} {}".format(n_name, sc.cp(file, n_name)))

def make_subdir_copies(file, n_dirs, n_copies):
    for i in range(n_dirs):
        subdir = "{}/data_{:04}".format(os.path.dirname(file), i)
        print ('Making ' + subdir + ' ' + sc.mkdir(subdir))
        make_copies(file, subdir, n_copies)
        timed_ls(subdir+'/')

def test_copies(fname, n_files, n_dirs):
    tmpf = tempfile.NamedTemporaryFile(delete=False)
    tmpf.close()
    try:
        print ('Making data dir ' + sc.mkdir('vos://data'))
        print ('Copying ' + fname + ' ' + sc.put(fname, 'vos://data/'))
        make_copies ('vos://data/' + os.path.basename(fname), 'vos://data', n_files)
        make_subdir_copies ('vos://data/' + os.path.basename(fname), n_dirs, n_files)
        timed_ls ('vos://data/')
        print (sc.get('vos://data/' + os.path.basename(fname), tmpf.name))
        print ('GET same as PUT: ' + str(filecmp.cmp(tmpf.name, fname, shallow=False)))
    except KeyboardInterrupt:
        pass
    finally:
        print ('Removing temp data: ' + sc.rmdir ('vos://data'))
        os.remove(tmpf.name)
        print (timed_ls ('vos://'))

def list_roots():
    for line in sys.stdin:
        line = line.rstrip()
        if ac.isValidUser (line): timed_ls (line + '://')
        else: print ("Invalid user " + line)

def main():
    host = os.uname().nodename.split('.')[0]
    defhost = os.uname().nodename if host is 'dldev' or host is 'dltest' else None

    parser = argparse.ArgumentParser(description='Test the VOSpace and storage manager.')
    parser.add_argument('-u', '--username', default=os.environ['USER'], help="username")
    parser.add_argument('-c', '--copyfile', default=None, help="file to copy")
    parser.add_argument('-n', '--hostname', default=defhost, help="storage manager hostname")
    parser.add_argument('--nfiles', default=5, help="number of files to create")
    parser.add_argument('--ndirs', default=5, help="number of subdirectories to create")
    parser.add_argument('-s', '--subdir', default='data', help="subdirectory name to create")
    parser.add_argument('-f', '--fileservice', action='append', default=[], help="file services to list")
    parser.add_argument('-p', '--print_list', action='store_true', help="print directory listings")
    parser.add_argument('-r', '--recurse', action='store_true', help="list file services recursively")
    args = parser.parse_args()

    if args.hostname:
        hostname = 'http://{}/'.format(args.hostname)
        print (hostname)
        ac.set_svc_url('{}/auth'.format(hostname))
        sc.set_svc_url('{}/storage'.format(hostname))
    while not ac.isUserLoggedIn(args.username):
        ac.login(args.username, getpass.getpass('Enter {} password (+ENTER): '.format(args.username)))
    else:
        try:
            timed_ls ('vos://', print_list=args.print_list)
            for f in args.fileservice:
                if args.recurse:
                    recursive_ls ('{}://'.format(f))
                else:
                    timed_ls ('{}://'.format(f), print_list=args.print_list)
        except KeyboardInterrupt:
            pass
        finally:
            if args.copyfile:
                test_copies(args.copyfile, args.nfiles, args.ndirs)

if __name__ == '__main__':
    main()
