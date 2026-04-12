
'''
@Copy right Visionary Imaging Services, Inc.
@since 2026
'''

from setuptools import setup

version_str = "${project.version}"
if version_str.startswith("${"):
    version_str = "0.0.0.dev0"

setup(
    name="radiomicsj",
    version=version_str,
    description="Python wrapper for RadiomicsJ",
    author="Tatsuaki Kobayashi",
    packages=["radiomicsj", "radiomicsj.jars"],
    package_data={
        "radiomicsj": ["jars/*.jar"],
    },
    include_package_data=True,
    install_requires=[
        "jpype1>=1.3.0",
        "numpy",
        "pandas"
    ],
)