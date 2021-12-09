import sys,argparse
import git
import re
import os
import subprocess

def parse_argument():
    parser = argparse.ArgumentParser(
        description = "Analyse options for gitDriller.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        '--repo',
        default = 'https://github.com.cnpmjs.org/alibaba/dragonwell8',
        help='git repo'
    )
    parser.add_argument(
        '--fromtag',
        default = None,
        help='traverse git commits from'
    )
    parser.add_argument(
        '--release',
        default = None,
        help='release version'
    )
    parser.add_argument(
        '--totag',
        default = None,
        help='traverse git commits to'
    )
    parser.add_argument(
        '--hash',
        default = None,
        help='git commit hash'
    )
    args = parser.parse_args()
    return args

if __name__ == "__main__":
    args = parse_argument()
    table_data = []
    paths = [""]
    if ("dragonwell8" in args.repo):
        paths.append(["jdk", "hotspot"])
    for path in paths:
        repo_dir = os.path.join(args.repo, path)
        repo = git.Repo(repo_dir)
        if (args.fromtag is None):
            revstr = "master"
        else:
            revstr = "{}...master".format(args.fromtag)
        for commit in repo.iter_commits(rev=revstr):
            if re.match(r"\[(Misc|Wisp|GC|Backport|JFR|Runtime|Coroutine|Merge|JIT|RAS|JWarmUp|JWarmUp)", commit.summary) != None:
                print (commit.tree)
