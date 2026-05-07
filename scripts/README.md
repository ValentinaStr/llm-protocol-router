# scripts

This folder contains helper scripts for working with the OpenSpec repository.

create-branch-from-task.ps1
- Scans a markdown tasks file for the first unchecked task (`- [ ] ...`).
- Creates a git branch named `task/<slug>` where `<slug>` is derived from the task text.
- Optionally marks the task in the file as in-progress and appends the branch name.

Usage (PowerShell):

```powershell
# create branch from default tasks file
./create-branch-from-task.ps1

# create branch and mark the task line in the file
./create-branch-from-task.ps1 -TaskFile openspec/changes/enterprise-llm-router/tasks.md -MarkFile
```

Notes:
- Script expects `git` available in PATH and to be run from repository root or a git working directory.
- The default task file is `openspec/changes/enterprise-llm-router/tasks.md` — change the `-TaskFile` argument to point to another file.
