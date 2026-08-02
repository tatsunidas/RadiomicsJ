/*
 * Copyright [2026] [Tatsuaki Kobayashi]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package radiomics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

/**
 * A digital phantom used to validate the GLAM implementation.
 *
 * The phantom files are plain text: a small header, then one line of gray level
 * indices per image row, with -1 marking a voxel outside the roi. They are
 * produced by the python side of the validation, together with the reference
 * values of every affinity matrix.
 *
 * The voxel intensity is the gray level index itself, so a fixed bin number
 * discretisation with exactly that many bins reproduces the intended levels.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public class GLAMPhantom {

	public final String name;
	public final int width;
	public final int height;
	public final int depth;
	public final int levels;
	public final int maxRadius;
	/** Gray level index per voxel, -1 outside the roi. */
	public final int[][][] voxels;

	private GLAMPhantom(String name, int width, int height, int depth, int levels, int maxRadius, int[][][] voxels) {
		this.name = name;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.levels = levels;
		this.maxRadius = maxRadius;
		this.voxels = voxels;
	}

	public static GLAMPhantom load(String name) throws IOException {
		String resource = "glam/" + name + ".phantom";
		try (InputStream in = GLAMPhantom.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("GLAM phantom not found on the classpath: " + resource);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			int width = 0, height = 0, depth = 0, levels = 0, maxRadius = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.equals("data")) {
					break;
				}
				String[] parts = line.split("\\s+");
				switch (parts[0]) {
				case "width":
					width = Integer.parseInt(parts[1]);
					break;
				case "height":
					height = Integer.parseInt(parts[1]);
					break;
				case "depth":
					depth = Integer.parseInt(parts[1]);
					break;
				case "levels":
					levels = Integer.parseInt(parts[1]);
					break;
				case "maxRadius":
					maxRadius = Integer.parseInt(parts[1]);
					break;
				default:
					throw new IOException("unknown phantom header entry: " + line);
				}
			}
			int[][][] voxels = new int[depth][height][width];
			for (int z = 0; z < depth; z++) {
				for (int y = 0; y < height; y++) {
					String row = reader.readLine();
					String[] parts = row.trim().split("\\s+");
					for (int x = 0; x < width; x++) {
						voxels[z][y][x] = Integer.parseInt(parts[x]);
					}
				}
			}
			return new GLAMPhantom(name, width, height, depth, levels, maxRadius, voxels);
		}
	}

	/**
	 * Image whose intensities are the gray level indices themselves.
	 */
	public ImagePlus image() {
		ImageStack stack = new ImageStack(width, height);
		for (int z = 0; z < depth; z++) {
			FloatProcessor fp = new FloatProcessor(width, height);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int level = voxels[z][y][x];
					fp.setf(x, y, level < 0 ? 0f : (float) level);
				}
			}
			stack.addSlice(fp);
		}
		ImagePlus imp = new ImagePlus(name, stack);
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}

	public ImagePlus mask(int label) {
		ImageStack stack = new ImageStack(width, height);
		for (int z = 0; z < depth; z++) {
			FloatProcessor fp = new FloatProcessor(width, height);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					fp.setf(x, y, voxels[z][y][x] < 0 ? 0f : (float) label);
				}
			}
			stack.addSlice(fp);
		}
		ImagePlus imp = new ImagePlus(name + "_mask", stack);
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}

	/**
	 * Reads a reference matrix produced by the python validation.
	 */
	public static double[][] loadMatrix(String resourceName) throws IOException {
		String resource = "glam/" + resourceName;
		try (InputStream in = GLAMPhantom.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("GLAM reference values not found on the classpath: " + resource);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			List<double[]> rows = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				String[] parts = line.split(",");
				double[] row = new double[parts.length];
				for (int i = 0; i < parts.length; i++) {
					row[i] = "NaN".equals(parts[i]) ? Double.NaN : Double.parseDouble(parts[i]);
				}
				rows.add(row);
			}
			return rows.toArray(new double[0][]);
		}
	}

	/**
	 * Reads a reference radial distribution function, indexed as g[r][alpha][beta].
	 */
	public static double[][][] loadRdf(String resourceName, int levels, int maxRadius) throws IOException {
		String resource = "glam/" + resourceName;
		try (InputStream in = GLAMPhantom.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("GLAM reference values not found on the classpath: " + resource);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			reader.readLine();// header
			double[][][] g = new double[maxRadius + 1][levels][levels];
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				String[] parts = line.split(",");
				int r = Integer.parseInt(parts[0]);
				int column = 1;
				for (int alpha = 0; alpha < levels; alpha++) {
					for (int beta = 0; beta < levels; beta++) {
						g[r][alpha][beta] = Double.parseDouble(parts[column++]);
					}
				}
			}
			return g;
		}
	}
}
