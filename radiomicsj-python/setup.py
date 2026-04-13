
'''
@Copy right Visionary Imaging Services, Inc.
@since 2026
'''

from setuptools import setup

version_str = "${project.version}"
if version_str.startswith("${"):
    version_str = "0.0.0.dev0"
    
# README.md の内容を読み込む
with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setup(
    name="radiomicsj",
    version=version_str,
    
    # 1. 概要と詳細説明の紐づけ
    description="Python wrapper for RadiomicsJ (Java-based radiomics feature extraction)",
    long_description=long_description,
    long_description_content_type="text/markdown", # ★これがないとPyPIで綺麗に表示されません
    
    # 2. リンク集の追加（PyPIの左サイドバーに表示されます）
    project_urls={
        "Bug Tracker": "https://github.com/tatsunidas/RadiomicsJ/issues",
        "Source Code": "https://github.com/tatsunidas/RadiomicsJ",
        "Documentation": "https://github.com/tatsunidas/RadiomicsJ/blob/master/README.md",
    },
    
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