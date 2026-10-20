"""
Install-and-run check of the radiomicsj wheel, used by .github/workflows/ci.yml.

    python installed_smoke.py <expected version> <ibsi_1_digital_phantom/nifti directory>

Run it with the python of an environment that holds only the wheel, from a directory away
from the source tree. It fails when
- radiomicsj is imported from anywhere but the installed package,
- the version is not the expected one, or the jar folder holds another radiomicsj jar,
- a feature of the IBSI 1 digital phantom misses its benchmark value.
"""
import glob
import os
import sys

import radiomicsj
from radiomicsj import calculate_features
from radiomicsj import core

# IBSI 1 benchmark values of the digital phantom (IBSI_ValidationFile.xlsx, sheet "digital phantom"),
# with the tolerance of the benchmark or, where the benchmark gives none, its rounding
BENCHMARK = {
    "Morphology_VolumeMesh": (556.0, 4.0),
    "Morphology_VolumeVoxelCounting": (592.0, 4.0),
    "IntensityBasedStatistical_Mean": (2.15, 0.01),
    "IntensityBasedStatistical_Variance": (3.05, 0.01),
    "IntensityBasedStatistical_Skewness": (1.08, 0.01),
    "IntensityBasedStatistical_Kurtosis": (-0.355, 0.01),
    "IntensityBasedStatistical_Maximum": (6.0, 0.01),
}


def main(expected_version, phantom):
    package_dir = os.path.dirname(os.path.abspath(radiomicsj.__file__))
    print("radiomicsj imported from", package_dir)
    if "site-packages" not in package_dir:
        sys.exit("FAIL: radiomicsj is not the installed package: " + package_dir)

    if core.__version__ != expected_version:
        sys.exit("FAIL: version %s, expected %s" % (core.__version__, expected_version))
    jars = sorted(os.path.basename(j) for j in glob.glob(os.path.join(package_dir, "jars", "radiomicsj-*.jar")))
    if jars != ["radiomicsj-%s.jar" % expected_version]:
        sys.exit("FAIL: the jar folder holds %s" % jars)

    image = os.path.join(phantom, "image") + os.sep
    mask = os.path.join(phantom, "mask") + os.sep
    df = calculate_features(image, mask)
    if df.shape[0] != 1 or df.shape[1] < 100:
        sys.exit("FAIL: unexpected result of shape %s" % (df.shape,))
    failed = []
    for column, (benchmark, tolerance) in BENCHMARK.items():
        if column not in df.columns:
            failed.append(column + " (missing)")
            continue
        value = float(df.iloc[0][column])
        ok = abs(value - benchmark) <= tolerance
        print("%-40s %12.6g  benchmark %10.6g +- %-6g %s" % (column, value, benchmark, tolerance, "ok" if ok else "OUT"))
        if not ok:
            failed.append(column)
    if failed:
        sys.exit("FAIL: " + ", ".join(failed))
    print("OK: %d features, version %s" % (df.shape[1], expected_version))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
