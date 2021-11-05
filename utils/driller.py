import sys,argparse
import git
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
        '--totag',
        default = None,
        help='traverse git commits to'
    )
    args = parser.parse_args()
    return args

if __name__ == "__main__":
    args = parse_argument()
    table_data = []
    repo = git.Repo(args.repo)
    revstr = ""
    revstr += args.totag
    if args.fromtag != None:
        revstr += "..."
        revstr += args.fromtag
    for commit in repo.iter_commits(rev=revstr):
        cr_link=""
        issue_link=""
        for line in commit.message.split("\n"):
            if 'CR' in line:
                cr_link = line
            if 'Issue' in line:
                issue_link = line
        if issue_link != "":
            table_data.append([commit.summary, cr_link ])
    writer = MarkdownTableWriter(
        table_name="release_notes",
        headers=["summary", "cr"],
        value_matrix=table_data
    )
    writer.write_table()
