import HtmlTestRunner
import unittest
import json
from types import FunctionType, CodeType

results = json.load(open('release.json'))
print results
print results.get("checkGithubRleaseArtifactsSum")

# TODO: enable check_tag shell
class TestStringMethods(unittest.TestCase):
    """ Example test for HtmlRunner. """
    def test_checkGithubReleaseArtifactsName(self):
        """ This test should fail. """
        print results.get("checkGithubReleaseArtifactsName")
        self.assertTrue(results.get("checkGithubReleaseArtifactsName").get("result"))

code_template = """
def test_template(self):
    print results.get(\"NAME\")
    self.assertTrue(results.get(\"NAME\").get("result"))
"""

if __name__ == '__main__':
    for k in results:
        code_template = code_template.replace("NAME", k)
        print code_template
        foo_compile = compile(code_template , "", "exec")
        foo_code = [ i for i in foo_compile.co_consts if isinstance(i, CodeType)][0]
        testName = "test_"+k
        setattr(TestStringMethods, testName, FunctionType(foo_code, globals()))
    unittest.main(testRunner=HtmlTestRunner.HTMLTestRunner())
