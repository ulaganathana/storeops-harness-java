# storeops-harness-java

StoreOps retail operations API (Spring Boot 3, Java 17, Maven) governed by a Claude Code harness (Planner -> Generator <-> Evaluator -> Monitor).
Harness files live in `.harness/`, CI in `.github/`. Maven's `src/test/java/com/storeops/<module>/` is this project's `tests/` (mirrors `src/`).

## Runbook (do in this order)

### 1. Verify and commit the baseline (the scaffold was authored without a Maven run)
```bash
mvn -N wrapper:wrapper                 # creates ./mvnw (+ .mvn/)
mvn -B verify                          # Checkstyle + tests + ArchUnit + JaCoCo + SpotBugs -> BUILD SUCCESS
./mvnw spring-boot:run &               # new shell:
curl -i -H 'X-Staff-Id: s-1' http://localhost:8080/api/activities    # 200 OK  (PDF says /api/tasks; this API's path is /api/activities)
```
Fix every failure; log each correction in `JOURNAL.md`; then `git commit -am "baseline: verified"`.

### 2. Prove the gates bite (evidence for REFLECTION / Brief section C)
```bash
python3 .harness/scripts/collect-gates.py && .harness/scripts/compute-verdict.sh        # expect VERDICT: PASS
# temporarily add `import com.storeops.activities.repository.ActivityRepository;` to a programmes class, re-run -> A1 FAIL, VERDICT: FAIL
git checkout -- src && cp .harness/output/checks.env /tmp/   # keep the FAIL output as an example if useful
```

### 3. Run the harness (the real demonstration run)
```bash
claude                                  # reads CLAUDE.md
> @planner <contents of PROMPT.md>      # review .harness/output/spec.md
> APPROVED                              # then do not intervene unless .harness/output/escalation.md appears
```
When finished, `.harness/reviews/` must contain: `spec.md`, `sprint-N-contract.md`, `sprint-N-generator-summary.md`, `sprint-N-evaluator-feedback.md`, `sprint-N-run-log.md`.

### 4. Deploy and capture evidence
```bash
docker compose up --build
curl -i -X PATCH http://localhost:8080/api/activities/bulk-status -H 'X-Staff-Id: s-3' -H 'Content-Type: application/json' -d '<body from your contract>'
```
Screenshot the container + response. `cp templates/DEPLOYMENT.md.template DEPLOYMENT.md` and fill from reality; same for `REFLECTION.md`.

### 5. Finish documents, then gate yourself
Replace the Section A example in `DESIGN_BRIEF.md` with the real `sprint-1-contract.md` entry, delete the DRAFT banner, finish `JOURNAL.md`.
```bash
.harness/scripts/verify-submission.sh   # every FAIL is a missing requirement; exit 0 = all PDF requirements satisfied
```
