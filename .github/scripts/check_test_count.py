"""Fail when the test run silently ran fewer tests than the sources declare.

A dependency update once made surefire run 0 tests while the build stayed green.
Expected: the @Test methods of the files surefire picks up by name
(Test*.java, *Test.java, *Tests.java, *TestCase.java) under src/test/java.
Executed:  the sum of `tests` in target/surefire-reports/TEST-*.xml.
Skipped tests also fail the check.
@ParameterizedTest, @RepeatedTest and @TestFactory are not counted; if they are used,
change the way the expected number is counted here.

usage: python check_test_count.py [project root]
"""
import fnmatch
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

PATTERNS = ["Test*.java", "*Test.java", "*Tests.java", "*TestCase.java"]
TEST = re.compile(r"^\s*@Test\b", re.MULTILINE)


def expected(root):
    n = 0
    for dirpath, _, files in os.walk(os.path.join(root, "src", "test", "java")):
        for f in files:
            if any(fnmatch.fnmatch(f, p) for p in PATTERNS):
                with open(os.path.join(dirpath, f), encoding="utf-8") as fh:
                    n += len(TEST.findall(fh.read()))
    return n


def executed(root):
    reports = glob.glob(os.path.join(root, "target", "surefire-reports", "TEST-*.xml"))
    tests = skipped = 0
    for r in reports:
        suite = ET.parse(r).getroot()
        tests += int(suite.get("tests", 0))
        skipped += int(suite.get("skipped", 0))
    return len(reports), tests, skipped


def main(root):
    want = expected(root)
    reports, ran, skipped = executed(root)
    print("test count: declared %d, executed %d, skipped %d, in %d report files" % (want, ran, skipped, reports))
    if reports == 0 or want == 0:
        sys.exit("FAIL: no test reports or no declared tests")
    if ran < want:
        sys.exit("FAIL: %d tests were declared but only %d ran" % (want, ran))
    if skipped:
        sys.exit("FAIL: %d tests were skipped" % skipped)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
