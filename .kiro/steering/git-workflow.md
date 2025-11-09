# Git Workflow Conventions

This guide defines conventions for branch naming, commit messages, and PR descriptions when working with spec-driven development.

## Branch Naming

When working on spec tasks, use descriptive branch names that capture the essence of the work:

### Format
```
feature/[key-concept]
```

### Guidelines
- Extract the core concept from the spec name or task group
- Use kebab-case (lowercase with hyphens)
- Keep it concise but meaningful (2-4 words ideal)
- Avoid redundant words like "implementation" or "feature"

### Examples
- Spec: `alarm-state-recovery-and-stability`, Tasks 6-10 → `feature/alarm-recovery-stability`
- Spec: `user-authentication`, Tasks 1-3 → `feature/auth-setup`
- Spec: `notification-improvements`, Tasks 4-6 → `feature/notification-enhancements`

### Task Grouping
When multiple tasks are mentioned:
- Group related tasks that form a logical unit of work
- Suggest a branch name that encompasses all tasks
- Consider: Do these tasks depend on each other? Do they touch the same components?

## Commit Messages

Use conventional commits format with spec and task references:

### Format
```
<type>(<scope>): <description>

[optional body with more details]

Spec: <spec-name>
Tasks: #<task-number>, #<task-number>
```

### Types
- `feat`: New feature implementation
- `fix`: Bug fix
- `refactor`: Code refactoring without behavior change
- `test`: Adding or updating tests
- `docs`: Documentation changes
- `chore`: Build, dependencies, or tooling changes

### Examples
```
feat(alarm): implement boot receiver for alarm restoration

Add BootReceiver to restore active alarms after device reboot.
Handles alarm state recovery and reschedules next ring time.

Spec: alarm-state-recovery-and-stability
Tasks: #6.1, #6.2
```

```
feat(scheduler): add doze mode recovery logic

Implement recovery mechanism for alarms missed during Doze mode.
Calculates catch-up rings and reschedules appropriately.

Spec: alarm-state-recovery-and-stability
Tasks: #7.1, #7.2, #7.3
```

## Pull Request Descriptions

### Template
```markdown
## Overview
[Brief description of what this PR accomplishes]

## Spec Reference
**Spec**: `<spec-name>`
**Location**: `.kiro/specs/<spec-name>/`

## Completed Tasks
- [x] Task #X.X: [Task description]
- [x] Task #X.X: [Task description]
- [x] Task #X.X: [Task description]

## Implementation Details
[Key technical decisions, approach, or notable changes]

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Edge cases verified

## Requirements Addressed
- Requirement X.X: [Brief description]
- Requirement X.X: [Brief description]

## Notes
[Any additional context, known limitations, or follow-up items]
```

### Example
```markdown
## Overview
Implements alarm recovery mechanisms for boot, Doze mode, and app updates to ensure alarm reliability across system events.

## Spec Reference
**Spec**: `alarm-state-recovery-and-stability`
**Location**: `.kiro/specs/alarm-state-recovery-and-stability/`

## Completed Tasks
- [x] Task #6: Implement boot receiver for alarm restoration
- [x] Task #7: Implement Doze mode recovery
- [x] Task #8: Implement app update recovery
- [x] Task #9: Add alarm state validation
- [x] Task #10: Implement recovery logging

## Implementation Details
- Added BootReceiver with RECEIVE_BOOT_COMPLETED permission
- Implemented DozeRecoveryManager to handle missed alarms during Doze
- Created AlarmStateValidator for consistency checks
- Added comprehensive logging for recovery events
- All recovery logic integrated with existing AlarmScheduler

## Testing
- [x] Unit tests for recovery logic
- [x] Manual testing: device reboot, Doze mode simulation
- [x] Edge cases: multiple missed rings, state corruption scenarios

## Requirements Addressed
- Requirement 3.1: Boot recovery within 30 seconds
- Requirement 3.2: Doze mode recovery on exit
- Requirement 3.3: App update alarm preservation
- Requirement 4.1: State validation on recovery

## Notes
Recovery logging uses debug level by default. Can be enabled via settings for troubleshooting.
```

## Best Practices

1. **Branch Names**: When asked for a branch name, analyze the tasks and suggest a name that captures their common theme
2. **Commit Frequency**: Commit after completing each logical unit (typically one sub-task)
3. **PR Scope**: Keep PRs focused on related tasks from the same spec
4. **Spec References**: Always include spec name and task numbers for traceability
5. **Testing Notes**: Document what testing was performed, especially for critical features like alarm recovery
