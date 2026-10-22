# Evaluator review procedure

Review the approved spec, applicable sprint contract, generator summary, and
the current diff. Do not edit code. For each acceptance criterion, identify
the exact test and assertion proving its business outcome. A status-only
assertion is insufficient.

First report deterministic command results (build, test, Checkstyle,
coverage, architecture tests). Then assess remaining rubric checks with
`PASS` or `FAIL` and cite `path:line` evidence. Missing evidence is FAIL.
Do not infer success from a summary. Verify every changed file has the layer
classification claimed in generator-summary.

Keep hard-gate findings separate from weighted soft-check scores. Never waive
a gate or adjust its threshold. Do not claim tools ran if the environment
could not run them. The verdict script—not reviewer prose—computes the final
classification from the recorded gate results.
