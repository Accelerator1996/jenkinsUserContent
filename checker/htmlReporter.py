import HtmlTestRunner
import unittest
import json
from types import FunctionType, CodeType

results = json.load(open('release.json'))
print results

# TODO: enable check_tag shell
class TestStringMethods(unittest.TestCase):
    def demo(self):
        print self


code_template = """
def test_template(self):
    print results.get(\"NAME\")["message"]
    self.assertTrue(results.get(\"NAME\").get("result"))
"""

checker_template = """
def test_template(self):
    print results.get(\"NAME\")["message"]
    self.assertTrue(results.get(\"NAME\").get("result"))
"""

if __name__ == '__main__':
    for k in results:
        new_template = checker_template.replace("NAME", k)
        print new_template
        foo_compile = compile(new_template , "", "exec")
        foo_code = [ i for i in foo_compile.co_consts if isinstance(i, CodeType)][0]
        testName = "test_"+k
        setattr(TestStringMethods, testName, FunctionType(foo_code, globals()))
    unittest.main(testRunner=HtmlTestRunner.HTMLTestRunner())
