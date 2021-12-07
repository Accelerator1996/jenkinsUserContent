import sys,argparse
import git
import re
import os
import subprocess
from pytablewriter import MarkdownTableWriter

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
    args = parser.parse_args()
    return args

def exec_shell(cmd, cwd=".", timeout=120, display=False):
    sb = subprocess.Popen(cmd,
                          cwd=cwd,
                          stdin=subprocess.PIPE,
                          stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE,
                          shell=True
                          )
    if display:
        log.info("exec shell command in {}: {}".format(cwd, cmd))
    out, err, retv = "", "", -999
    try:
        out, err = sb.communicate(timeout=timeout)
    except TimeoutExpired:
        sb.kill()
    finally:
        retv = sb.returncode
        return out, err, retv

if __name__ == "__main__":
    args = parse_argument()
    table_data = []
    paths = ["", "jdk", "hotspot"]
    for path in paths:
        repo_dir = os.path.join(args.repo, path)
        repo = git.Repo(repo_dir)
        fromtag, err, retv = exec_shell("git tag --sort=committerdate | tail -n 2 | head -n 1", repo_dir)
        totag, err, retv = exec_shell("git tag --sort=committerdate | tail -n 1", repo_dir)
        fromtag = bytes.decode(fromtag).strip()
        totag = bytes.decode(totag).strip()
        revstr = "{}...{}".format(fromtag, totag)
        for commit in repo.iter_commits(rev=revstr):
            summary=""
            issue_link=""
            for line in commit.message.split("\n"):
                if 'Issue' in line:
                    if 'https' in line:
                        issue_numeber = line.split("issues/")[-1].strip()
                        issue_url = line.split("Issue:")[-1].strip()
                        issue_link = "[Issue #" + issue_numeber + "](" + issue_url  +")"
                    else:
                        issue_numeber = line.split("#")[-1].strip()
                        issue_url = "https://github.com/alibaba/dragonwell" +args.release + "/issues/" + issue_numeber
                        issue_link = "[Issue #" + issue_numeber + "](" + issue_url  +")"
            if re.match(r"\[(Misc|Wisp|GC|Backport|JFR|Runtime|Coroutine|Merge|JIT|RAS|JWarmUp|JWarmUp)", commit.summary) != None:
                table_data.append([commit.summary, issue_link])
    writer = MarkdownTableWriter(
        table_name="Release Notes",
        headers=["Summary", "Issue"],
        value_matrix=table_data
    )
    writer.write_table()
