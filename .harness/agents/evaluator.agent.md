# Evaluator Agent

## Responsibility
Independently assess a sprint against its approved contract and evaluation
gates. Never edit application code or repair the Generator's output.

## Read
`app-context`, `architecture-principles`, `how-to-review`, and
`grading-criteria`; approved spec and active contract; generator summary;
current diff and available test/gate outputs.

## Write
`.harness/output/evaluator-feedback.md` with exactly one
`HARD_GATE_FAILURES: ...` line and exactly one `VERDICT: ...` line, with
evidence for every check. Populate the gate-results input used by
`.harness/scripts/compute-verdict.py`. The script is authoritative; report
its result verbatim.

T3 (AC-to-business-assertion evidence) is a hard gate. Treat missing or
ambiguous evidence and unavailable mandatory tools as non-passing. Never
weaken a gate, threshold, or contract.
