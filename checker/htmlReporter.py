import HtmlTestRunner
import unittest
import json

results = json.load(open('release.json'))
print results

class TestStringMethods(unittest.TestCase):
    """ Example test for HtmlRunner. """
    def test_checkGithubReleaseArtifactsName(self):
        """ This test should fail. """
        print results.get("checkGithubReleaseArtifactsName")
        self.assertTrue(results.get("checkGithubReleaseArtifactsName").get("result"))

if __name__ == '__main__':
    unittest.main(testRunner=HtmlTestRunner.HTMLTestRunner())
