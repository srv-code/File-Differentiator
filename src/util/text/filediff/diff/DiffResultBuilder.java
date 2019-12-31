package util.text.filediff.diff;

import java.nio.file.Path;


public class DiffResultBuilder {	
	private long[] diffValue;
	private Path newerFile, olderFile;
	private DiffResult.Type diffType;
	private boolean areTextFiles;
	
	public DiffResultBuilder setFiles(final Path file1, final Path file2, final boolean areBinaries) {
		this.newerFile = file1;
		this.olderFile = file2;
		this.areTextFiles = areBinaries;
		
		return this;
	}

	public DiffResultBuilder setDiff(final DiffResult.Type diffType, final long diffValue) {
		return setDiff(diffType, new long[] { diffValue, -1L } );
	}
	
	public DiffResultBuilder setDiff(final DiffResult.Type diffType, final long[] diffValue) {
		this.diffType = diffType;
		this.diffValue = diffValue;

		return this;
	}
	
	public DiffResult build() {
		return new DiffResult(newerFile, olderFile, areTextFiles, diffType, diffValue);
	}
}