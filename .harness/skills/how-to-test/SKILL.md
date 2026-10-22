# How to test StoreOps changes

## Commands

- `mvn test`: compile and run unit/integration tests.
- `mvn verify`: Checkstyle plus tests and configured JaCoCo thresholds.
- `mvn spring-boot:run`: start the API on port 8080.
- `python .harness/scripts/verify-disabled-tests.py`: fail if tests are
  disabled or ignored.
- `python .harness/scripts/collect-gates.py`: run deterministic checks and
  write `.harness/output/check-results.csv`; unavailable tools remain BLOCKED.
- `docker build -t storeops:local .` and `docker run --rm -p 8080:8080
  storeops:local`: container smoke test when Docker is installed.

## Evidence expected

Tests assert business outcomes, not only status codes. For an activity batch,
cover mixed success/failure, subsequent-item processing, stable request-order
results, unsupported target status, empty/invalid input, missing and
cross-store activities, duplicate IDs as specified, and exactly one audit
entry per successful change. Verify the persisted status and actor/time audit
fields. Verify failed items leave state and audit history unchanged.

MockMvc tests must include `X-User-Id`, parse the response body, and cover
missing/unknown users plus store isolation. Test each service-level
`AppError` path. Add explicit null/empty edge cases only where accepted by the
request contract.

## Coverage and quality

JaCoCo thresholds: service 80%, routes 70%, shared utility 60%, overall 70%
line coverage. Never disable tests to get a green build. Run the narrow
relevant test during development and `mvn test` as the requested final test;
run `mvn verify` where coverage and lint confidence are required. Record the
exact command and result in run evidence; unavailable tools are BLOCKED, not
PASS.
