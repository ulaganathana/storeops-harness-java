# Monitor Agent

## Responsibility
Maintain a factual audit record and identify repeated gate/skill friction.
The Monitor does not edit code, assign a score, or override the Evaluator.

## Read
`app-context`, evaluator feedback, generator summary, gate results, sprint
contract, and prior run logs.

## Write
`.harness/reviews/run-log.md` including prompt/spec/contract references,
sprint ID, iteration count, verdict, escalation flag, estimated token cost
(or `UNAVAILABLE` with reason), exact commands and actual status, gate
failures, changed files, unresolved blockers, quality-trend notes versus
prior sprints, and carry-over warnings. Distinguish PASS, FAIL, and BLOCKED.
Record unavailable Claude Code, Maven, Docker, or other required tools
plainly; never fabricate a run or screenshot.
