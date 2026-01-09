---
applyTo: "**"
---

# Pull Request Guidelines

## Creating Pull Requests

### Draft Pull Requests (Mandatory)

All new pull requests MUST be created in **draft** mode. The agent should never open a non-draft ("ready for review") PR initially.

**Agent Rules:**

- Always use the GitHub MCP tools for PR creation (activate repository management tools when needed)
- Set `draft: true` when invoking pull request creation tools
- If a PR was accidentally opened as ready, immediately update it to draft and leave a comment noting the correction
- Do not convert out of draft automatically; wait for explicit user request or all readiness conditions met

**Creating a PR with GitHub MCP:**

1. Activate repository management tools: `activate_repository_management_tools`
2. Use `mcp_github_create_pull_request` with the following parameters:
   - `owner`: Repository owner (e.g., "TruvetaPublic")
   - `repo`: Repository name (e.g., "OpenToken")
   - `title`: PR title following conventional commit format
   - `head`: Source branch (e.g., "dev/username/feature-name")
   - `base`: Target branch (typically "develop")
   - `body`: PR description with summary, changes, and testing checklist
   - `draft`: **true** (mandatory)

**Example:**

```
mcp_github_create_pull_request(
  owner="TruvetaPublic",
  repo="OpenToken",
  title="feat: add new attribute validation",
  head="dev/username/new-validation",
  base="develop",
  body="## Summary\n...",
  draft=true
)
```

### PR Readiness Checklist

Before converting a PR from draft to ready for review, ensure:

- [ ] All CI checks passing
- [ ] Code coverage ≥80% for new code
- [ ] Both Java and Python implementations updated (if applicable)
- [ ] Tests added/updated for changes
- [ ] Documentation updated (README, JavaDoc, docstrings)
- [ ] Service registration files updated (if adding attributes/tokens)
- [ ] No secrets or sensitive data committed
- [ ] Jupyter notebook outputs cleared

### Standard PR Structure

**Title Format:** `<type>: <short summary>`

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

**Body Template:**

```markdown
## Summary

Brief overview of changes and motivation

## Changes

- Detailed change 1
- Detailed change 2

## Testing

- [ ] Test item 1
- [ ] Test item 2

## Files Changed

- `path/to/file1` (X lines)
- `path/to/file2` (Y lines)
```
