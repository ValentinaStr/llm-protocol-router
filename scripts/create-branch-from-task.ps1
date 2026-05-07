<#
.SYNOPSIS
    Create a git branch from the first unchecked task found in a markdown file.

.DESCRIPTION
    Scans a markdown task file for the first unchecked checklist item ("- [ ] ..." or "* [ ] ...").
    Creates a branch named `task/<slug>` where <slug> is a sanitized version of the task text.
    Optionally updates the task line in the file to mark it in-progress and append the branch name.

.PARAMETER TaskFile
    Path to the markdown file containing tasks. Defaults to 'openspec/changes/enterprise-llm-router/tasks.md'.

.PARAMETER MarkFile
    If provided, the script will mark the task line in the file as in-progress and append the branch name.

.EXAMPLE
    ./create-branch-from-task.ps1 -TaskFile openspec/changes/enterprise-llm-router/tasks.md -MarkFile

#>
param(
    [string]$TaskFile = "openspec/changes/enterprise-llm-router/tasks.md",
    [switch]$MarkFile
)

if (-not (Test-Path -Path $TaskFile)) {
    Write-Error "Task file not found: $TaskFile"
    exit 2
}

$content = Get-Content -Raw -LiteralPath $TaskFile -ErrorAction Stop

$pattern = '^[ \t]*[-*][ \t]*\[[ \t]*\][ \t]*(.+)$'
$matches = [regex]::Matches($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)

if ($matches.Count -eq 0) {
    Write-Host "No unchecked tasks found in $TaskFile"
    exit 0
}

$taskText = $matches[0].Groups[1].Value.Trim()

# Create a safe slug
$slug = $taskText.ToLower()
$slug = ($slug -replace "[^a-z0-9\s-]", "") -replace "\s+", "-"
$slug = $slug -replace "-{2,}", "-"
$slug = $slug.Trim('-')

if ([string]::IsNullOrWhiteSpace($slug)) {
    $slug = $(Get-Date -Format yyyyMMddHHmmss)
}

$branch = "task/$slug"

Write-Host "Selected task: $taskText"
Write-Host "Creating branch: $branch"

# Check if branch already exists
git rev-parse --verify $branch >$null 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Error "Branch '$branch' already exists. Aborting."
    exit 3
}

# Create branch
git checkout -b $branch
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create branch '$branch'."
    exit 4
}

if ($MarkFile.IsPresent) {
    # Replace only the first occurrence of the pattern
    $lines = Get-Content -LiteralPath $TaskFile
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            $orig = $lines[$i]
            $lines[$i] = $orig -replace '\[[ \t]*\]', '[~]' -replace '\s*$', " (branch: $branch)"
            break
        }
    }
    $lines | Set-Content -LiteralPath $TaskFile
    Write-Host "Marked task in $TaskFile"
}

Write-Host "Branch '$branch' created and checked out."
exit 0
