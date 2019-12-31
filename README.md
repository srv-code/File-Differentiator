# File Differentiator
## Synopsis
A utility program to check for any differences between two roots (can be files or root directories) and if so then points to differing element(s).
Supports several switches to modify the behavior of the program execution.

## Features
- Options to accept the two versions of file/root directories which needs to be compared.
- Option to treat any file as binary (default is for textual file) i.e. to force byte to byte comparison and also suppresses checks for line terminators.
- Options to accept regular expressions for accepting or ignoring file names encountered while checking.
- Option to show the full stack trace of errors, in case of any. (Default is to suppress the stack trace and just show the brief error information.)
- Option to show only the list of all modified file names suppressing the modification details.
- Option to check for changes in modification time also (Default check is only for content data).
- Option to suppress showing the relative paths beside the file names.
- Option to suppress showing the common/identical files found in the comparison.
- Option to suppress showing the modified files found in the comparison.
- Options to suppress showing the new/extra files present on either of the versions of the directory tree.
