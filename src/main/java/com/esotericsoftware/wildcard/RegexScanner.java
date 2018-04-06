
package com.esotericsoftware.wildcard;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

class RegexScanner {
	private final Path rootDir;
	private final List<Pattern> includePatterns;
	private final List<String> matches = new ArrayList(128);

	public RegexScanner (Path rootDir, List<String> includes, List<String> excludes) {
		if (rootDir == null) throw new IllegalArgumentException("rootDir cannot be null.");
		if (!rootDir.toFile().exists()) throw new IllegalArgumentException("Directory does not exist: " + rootDir);
		if (!rootDir.toFile().isDirectory()) throw new IllegalArgumentException("File must be a directory: " + rootDir);
		try {
			rootDir = rootDir.toRealPath();
		} catch (IOException ex) {
			throw new RuntimeException("OS error determining canonical path: " + rootDir, ex);
		}
		this.rootDir = rootDir;

		if (includes == null) throw new IllegalArgumentException("includes cannot be null.");
		if (excludes == null) throw new IllegalArgumentException("excludes cannot be null.");

		includePatterns = new ArrayList();
		for (String include : includes)
			includePatterns.add(Pattern.compile(include, Pattern.CASE_INSENSITIVE));

		List<Pattern> excludePatterns = new ArrayList();
		for (String exclude : excludes)
			excludePatterns.add(Pattern.compile(exclude, Pattern.CASE_INSENSITIVE));

		scanDir(rootDir);

		for (Iterator matchIter = matches.iterator(); matchIter.hasNext();) {
			String filePath = (String)matchIter.next();
			for (Pattern exclude : excludePatterns)
				if (exclude.matcher(filePath).matches()) matchIter.remove();
		}
	}

	private void scanDir (Path dir) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path path : stream) {
				for (Pattern include : includePatterns) {
					String filePath = rootDir.relativize(path).toString();
					if (include.matcher(filePath).matches()) {
						matches.add(filePath);
						break;
					}
				}
				if (path.toFile().isDirectory()) scanDir(path);
			}
		} catch (IOException e) {
			// exception
		}
	}

	public List<String> matches () {
		return matches;
	}

	public Path rootDir () {
		return rootDir;
	}

	public static void main (String[] args) {
		// System.out.println(new Paths("C:\\Java\\ls", "**"));
		List<String> includes = new ArrayList();
		includes.add("core[^T]+php");
		// includes.add(".*/lavaserver/.*");
		List<String> excludes = new ArrayList();
		// excludes.add("website/**/doc**");
		long start = System.nanoTime();
		List<String> files = new RegexScanner(Paths.get("..\\website\\includes"), includes, excludes).matches();
		long end = System.nanoTime();
		System.out.println(files.toString().replaceAll(", ", "\n").replaceAll("[\\[\\]]", ""));
		System.out.println((end - start) / 1000000f);
	}
}
