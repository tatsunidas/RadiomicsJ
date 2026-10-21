#!/usr/bin/env bash
# Build and run the GPU prototypes. Needs target/classes (mvn -o compile), target/classpath.txt-style classpath in $CP,
# and libOpenCL.so on LD_LIBRARY_PATH (many systems only have libOpenCL.so.1; a symlink in a private dir is enough).
# usage: CP=<maven runtime classpath> OCL_LIB=<dir with libOpenCL.so> ./run.sh <case> [repeats]
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
root="$here/../.."
jocl="$HOME/.m2/repository/org/jocl/jocl/2.0.5/jocl-2.0.5.jar"
out="${OUT:-$here/build}"
mkdir -p "$out"
javac -d "$out" -cp "$root/target/classes:$jocl:${CP:?set CP}" $(find "$here/src" -name '*.java')
LD_LIBRARY_PATH="${OCL_LIB:-}:${LD_LIBRARY_PATH:-}" exec java -Djava.awt.headless=true -Xmx6g \
  -cp "$out:$root/target/classes:$root/target/test-classes:$jocl:$CP" radiomics.gpu.GpuBench "$here/kernels" "$@"
