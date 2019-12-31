package util.text.filediff.handler.fs;

import util.text.filediff.diff.DiffResult;
import util.text.filediff.diff.DiffResultBuilder;
import util.text.filediff.handler.error.ErrorHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class FileSystemHandler {
	private Path newerRootDir, olderRootDir;
	private Map<Path, Path> allNewer    = new HashMap<>(), /* mappings of all files in newer dir - for internal use */
								allOlder    = new HashMap<>(), /* mappings of all files in older dir - for internal use */
								commons     = new HashMap<>(), /* mappings of common files in newer & older dirs */
								newNewer    = new HashMap<>(), /* mappings of unique/new files in newer dir */
								newOlder    = new HashMap<>(); /* mappings of unique/new files in older dir */
	private Map<Path, Object[]> mods        = new HashMap<>(); /* mappings of modified files - 2nd element: sub-element at 0 is absolute path & sub-element at 1 is DiffResult */

	private boolean rootIsDir;
	private boolean checkAsTextFiles;
	private ErrorHandler errHandler;
	private boolean checkMTime = false, checkSize = false, checkContents = true; // default values set

	private boolean patternToAccept = true; // default value set
	private List<Pattern> filterPatterns = null; // default value set
	
	public FileSystemHandler(   final ErrorHandler hErr, 
	                            final String newerPath, 
	                            final String olderPath, 
	                            final boolean checkAsTextFiles) {
		this.errHandler = Objects.requireNonNull(hErr, "Null error handler");
		this.newerRootDir = Paths.get(Objects.requireNonNull(newerPath, "Null first path value")).toAbsolutePath();
		this.olderRootDir = Paths.get(Objects.requireNonNull(olderPath, "Null second path value")).toAbsolutePath();
		this.checkAsTextFiles = checkAsTextFiles;
		
		if(Files.notExists(newerRootDir, LinkOption.NOFOLLOW_LINKS))
			throw new IllegalArgumentException("File not found: " + newerPath);
		if(Files.notExists(olderRootDir, LinkOption.NOFOLLOW_LINKS))
			throw new IllegalArgumentException("File not found: " + olderPath);
		
		if( (rootIsDir=Files.isDirectory(newerRootDir, LinkOption.NOFOLLOW_LINKS))
				!= Files.isDirectory(olderRootDir, LinkOption.NOFOLLOW_LINKS)) // checking for same file types
			throw new IllegalArgumentException("Paths provided are not of same types");
	}

	/**
	 * Adds patterns with accept/reject mode
	 * @param patterns All patterns.
	 * @param accept true value accepts the file names only with the matches, otherwise rejects.
	 * @return All invalid patterns
	 * */
	public List<String> addPatterns(final String[] patterns, final boolean accept) {
		patternToAccept = accept;

		List<String> invalidPatterns = new ArrayList<>();
		filterPatterns = new ArrayList<>();
		for(String patt : patterns) {
			try {
				filterPatterns.add(Pattern.compile(patt));
			} catch(PatternSyntaxException e) {
				invalidPatterns.add(patt);
			}
		}

		return invalidPatterns;
	}
	
	public void checkMTime(boolean val) {
		checkMTime = val;
	}
	
	public void checkSize(boolean val) {
		checkSize = val;
	}
	
	public void checkContents(boolean val) {
		checkContents = val;
	}
	
	public void process() throws IOException { // sets all resultant maps
		if(rootIsDir) {
			buildList(newerRootDir, allNewer);
			buildList(olderRootDir, allOlder);
			Map<Path, Path> all = new HashMap<>(allNewer);
			all.putAll(allOlder); // add mappings from older dir also

			for(Map.Entry<Path, Path> entry : all.entrySet()) {
				boolean hasInNewer = allNewer.containsKey(entry.getKey());
				boolean hasInOlder = allOlder.containsKey(entry.getKey());

				if(hasInNewer && hasInOlder) { // common filenames in both, will go either in commons or in mods
					boolean newerIsDir = Files.isDirectory(allNewer.get(entry.getKey()));
					boolean olderIsDir = Files.isDirectory(allOlder.get(entry.getKey()));

					if(newerIsDir && olderIsDir) { // both are dirs
						commons.put(entry.getKey(), entry.getValue()); // include directly, no need of further checking
					} else if(!newerIsDir && !olderIsDir) { // both are regular files: presumably
						/* sending absolute paths from each dirs: newer & older */
						DiffResult result = getFileDiff(allNewer.get(entry.getKey()), allOlder.get(entry.getKey()));

						if(result.getType() == DiffResult.Type.NONE)
							commons.put(entry.getKey(), entry.getValue());
						else
							mods.put(entry.getKey(), new Object[] { entry.getValue(), result });
					} else { // of diff types but of same relative names
						Path newerFullPath = allNewer.get(entry.getKey());
						Path olderFullPath = allOlder.get(entry.getKey());

						mods.put(entry.getKey(),
								new Object[] { entry.getValue(),
												new DiffResultBuilder()
														.setFiles(newerFullPath, olderFullPath, checkAsTextFiles)
															.setDiff(DiffResult.Type.FTYPE,
																	new long[] {Files.isDirectory(newerFullPath) ? 1 : 0,
																				Files.isDirectory(olderFullPath) ? 1 : 0})
																.build()} );
					}
				} else { // file is new in either of the dirs: newer or older
					if (hasInNewer)
						newNewer.put(entry.getKey(), allNewer.get(entry.getKey()));
					else
						newOlder.put(entry.getKey(), allOlder.get(entry.getKey()));
				}
			}
		} else { // return single file diff
			try {
				DiffResult result = getFileDiff(olderRootDir, newerRootDir);
				if(result.getType() == DiffResult.Type.NONE)
					commons.put(olderRootDir, newerRootDir); // put both files for returning
				else
					mods.put(olderRootDir, new Object[] { newerRootDir, result });
			} catch(Exception e) {
				errHandler.add(e);
			}
		}
	}

	/* map getters - sends unmodifiable sets */
	public Set<Map.Entry<Path, Object[]>> getMods() {
		return Collections.unmodifiableSet(new TreeMap<>(mods).entrySet());
	}
	
	public Set<Map.Entry<Path, Path>> getCommons() {
		return Collections.unmodifiableSet(new TreeMap<>(commons).entrySet());
	}

	public Set<Map.Entry<Path, Path>> getNewInNewer() {
		return Collections.unmodifiableSet(new TreeMap<>(newNewer).entrySet());
	}

	public Set<Map.Entry<Path, Path>> getNewInOlder() {
		return Collections.unmodifiableSet(new TreeMap<>(newOlder).entrySet());
	}

	/**
	 * @param root Requires to be an absolute path
	 * */
	private void buildList(final Path root, final Map<Path, Path> map) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				try {
					nextFile:
					for(Path file : Files.newDirectoryStream(dir)) {
						if (filterPatterns == null) // no patterns provided, add all
							map.put(root.relativize(file), file.toAbsolutePath());
						else {
							for (Pattern pattern : filterPatterns) {
								if (pattern.matcher(file.getFileName().toString()).matches()) {
									if (patternToAccept)
										map.put(root.relativize(file), file.toAbsolutePath());
									continue nextFile;
								}
							}
							if (!patternToAccept)
								map.put(root.relativize(file), file.toAbsolutePath());
						}
					}
				} catch(IOException e) {
					errHandler.add("Cannot list directory: " + dir.toAbsolutePath(), e);
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}

	private DiffResult getFileDiff(final Path fileNewer, final Path fileOlder) throws IOException {
		/* Checking precedence: 
				- File size
				- File contents
				- File modtime
		*/
		DiffResultBuilder resultBuilder = new DiffResultBuilder();
		resultBuilder.setFiles(fileNewer, fileOlder, checkAsTextFiles);
		long valNewer, valOlder;
		
		if(checkSize) { /* compares file sizes */
			valNewer = Files.size(fileNewer);
			valOlder = Files.size(fileOlder);
			
			if(valNewer != valOlder) {
				resultBuilder.setDiff(DiffResult.Type.SIZE, valNewer > valOlder ? valNewer : -valOlder);
				return resultBuilder.build();
			}
		}
		
		if(checkContents) { /* compares file contents byte to byte */
			try (	InputStream istreamFileNewer = Files.newInputStream(fileNewer);
					InputStream istreamFileOlder = Files.newInputStream(fileOlder)) {
				int newerFileByte, olderFileByte;
				long lineCount=1L, byteCount=1L;
				while(((newerFileByte=(istreamFileNewer.read())) != -1 & (olderFileByte=(istreamFileOlder.read())) != -1) 
						&& newerFileByte == olderFileByte) {
					byteCount++;
					
					if(checkAsTextFiles) { /* Count for columns (named here as 'byteCount') also */ 
						if (newerFileByte == '\n') {
							lineCount++;
							byteCount = 1L; // resets previous value
						}
					}
				}

				if(newerFileByte != -1 && olderFileByte != -1) { /* Both not reached EOF */
					if(checkAsTextFiles) {
						if ((newerFileByte == '\r' || newerFileByte == '\n')
								&& (olderFileByte == '\r' || olderFileByte == '\n')) {
							/* Check for new line char mismatch */
							resultBuilder.setDiff(DiffResult.Type.NEWLINE, new long[]{lineCount, byteCount});
						} else {
							resultBuilder.setDiff(DiffResult.Type.BYTE, new long[]{lineCount, byteCount});
						}
					} else {
						resultBuilder.setDiff(DiffResult.Type.BYTE, byteCount);
					}
					return resultBuilder.build();
				}
			}
		}

		if(checkMTime) { /* compares file modtime */
			valNewer = Files.getLastModifiedTime(fileNewer, LinkOption.NOFOLLOW_LINKS).toMillis();
			valOlder = Files.getLastModifiedTime(fileOlder, LinkOption.NOFOLLOW_LINKS).toMillis();
			
			if(valNewer != valOlder) {
				resultBuilder.setDiff(DiffResult.Type.MODTIME, valNewer > valOlder ? valNewer : -valOlder);
				return resultBuilder.build();
			}
		}
		
		return resultBuilder.setDiff(DiffResult.Type.NONE, 0).build(); // returns diff of NONE type
	}
}