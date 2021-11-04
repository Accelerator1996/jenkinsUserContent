import jenkins
import sys
import json

"""
// All
// sanity.openjdk
// extended.openjdk

// Linux
// sanity.system
// sanity.functional
// TODO: reenable sanity.external on 17
// specjbb2015

// 8-Sepcial
// 8-TCK
"""

version = "8"
if version == "17":
    platforms = [{'name': 'aarch64_linux',
                  'tests': ['sanity.openjdk', 'extended.openjdk', 'sanity.system', 'sanity.functional']},
                 {'name': 'x86-64_linux',
                  'tests': ['sanity.openjdk', 'extended.openjdk', 'sanity.system', 'sanity.functional']},
                 {'name': 'x86-64_windows',
                  'tests': ['sanity.openjdk', 'extended.openjdk']}]
else:
    platforms = [{'name': 'aarch64_linux',
                  'tests': ['sanity.openjdk', 'extended.openjdk', 'sanity.system', 'sanity.functional']},
                 {'name': 'x86-64_linux',
                  'tests': ['sanity.openjdk', 'extended.openjdk', 'sanity.system', 'sanity.functional']},
                 {'name': 'x86-64_windows',
                  'tests': ['sanity.openjdk', 'extended.openjdk']}]

results={}

for item in platforms:
    platform = item['name']
    tests = item['tests']
    for test in tests:
        server = jenkins.Jenkins('http://ci.dragonwell-jdk.io/', username='Alibaba_Dragonwell',
                                 password='wqpnuGK4HfwCRWMe')
        jobName = 'Test_openjdk' + version + '_dragonwell_' + test + '_' + platform
        number = server.get_job_info(jobName)["lastBuild"]['number']
        build_info = server.get_build_info(jobName, number)
        print jobName, build_info['result']
        reportName = "/lastBuild/tapTestReport/"
        if test.find("openjdk") != -1:
            reportName = "/lastBuild/testReport/"
        report = "<a href=\"http://ci.dragonwell-jdk.io/job/" + jobName + reportName + "\">测试报告链接</a>"
        if (build_info['result'] == "SUCCESS"):
            results[jobName.replace('.', '_')] = {
                'result': True,
                'message': report
            }
        else:
            results[jobName.replace('.', '_')] = {
                'result': False,
                'message': report
            }

file = open('release.json', 'w')
file.write(json.dumps(results))
