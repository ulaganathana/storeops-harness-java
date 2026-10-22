# Grading criteria

Two dimensions are weighted to 100%:

| Dimension | Weight | Evidence |
|---|---:|---|
| D1 Architectural Compliance | 55% | module/layer checks, error handling, audit ownership |
| D2 Test and Build Quality | 45% | build, coverage, business assertions, test hygiene |

Hard gates are non-compensatory: any failed hard gate forces FAIL irrespective
of score. Each check is binary. Within each dimension, the score is the
fraction of passing checks multiplied by its weight; total score is the sum
of weighted dimension scores. PASS is >=85; CONDITIONAL PASS is >=70 and <85;
below 70 is FAIL. Record every result and evidence location. An unavailable
automated tool is BLOCKED and cannot count as passing.

Hard automated checks include module boundaries/raw throws (D1) and build,
coverage, and disabled-test checks (D2). The acceptance-criteria business
assertion check T3 is a hard evidence gate. Failed soft checks reduce the
score; they do not change hard-gate behavior.
