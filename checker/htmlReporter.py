import HtmlTestRunner
import unittest
import json

results = json.load(open('release.json'))

# TODO: enable check_tag shell
class TestStringMethods(unittest.TestCase):
    """ Example test for HtmlRunner. """
    def test_checkGithubReleaseArtifactsName(self):
        """ This test should fail. """
        print results.get("checkGithubReleaseArtifactsName")
        self.assertTrue(results.get("checkGithubReleaseArtifactsName").get("result"))

    def test_checkGithubRleaseArtifactsSum(self):
        print results.get("checkGithubRleaseArtifactsSum")
        self.assertTrue(results.get("checkGithubRleaseArtifactsSum").get("result"))

    def test_LinuxX64AlpineCheckSumValidate(self):
        print results.get("LinuxX64AlpineCheckSumValidate")
        self.assertTrue(results.get("LinuxX64AlpineCheckSumValidate").get("result"))

    def test_LinuxX64CheckSumValidate(self):
        print results.get("LinuxX64CheckSumValidate")
        self.assertTrue(results.get("LinuxX64CheckSumValidate").get("result"))

    def test_WindowsCheckSumValidate(self):
        print results.get("WindowsCheckSumValidate")
        self.assertTrue(results.get("WindowsCheckSumValidate").get("result"))

    def test_LinuxAarch64CheckSumValidate(self):
        print results.get("LinuxAarch64CheckSumValidate")
        self.assertTrue(results.get("LinuxAarch64CheckSumValidate").get("result"))

if __name__ == '__main__':
    unittest.main(testRunner=HtmlTestRunner.HTMLTestRunner())
