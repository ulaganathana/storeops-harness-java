# storeops-harness-java

StoreOps retail operations API (Spring Boot 3, Java 17) governed by a Claude Code harness (Planner → Generator ⇄ Evaluator → Monitor).

## Prerequisites
JDK 17+, Maven 3.9+, Git, Docker (for deployment), Claude Code (`npm i -g @anthropic-ai/claude-code`).

## 1. Verify the baseline (first thing to do)
```bash
mvn -N wrapper:wrapper          # generates ./mvnw (the scaffold ships without it)
mvn -B verify                   # Checkstyle + tests + ArchUnit + JaCoCo + SpotBugs
./mvnw spring-boot:run &        # then:
curl -i http://localhost:8080/api/activities    # 200 OK
```
Fix anything that fails (see JOURNAL.md — log each correction), then commit. The scaffold was written without being able to run Maven; expect a few small fixes.

## 2. Run the harness
```bash
cd storeops-harness-java && claude          # reads CLAUDE.md automatically
> @planner <contents of PROMPT.md>
> APPROVED                                   # after reviewing .harness/output/spec.md
```
Claude Code then runs Generator/Evaluator/Monitor until all sprints pass or `.harness/output/escalation.md` appears.

## 3. Evaluator tooling (also runnable by hand)
```bash
python3 .harness/scripts/collect-gates.py     # runs mvn verify, writes .harness/output/checks.env + gate-details.txt
.harness/scripts/compute-verdict.sh           # deterministic verdict from checks.env
```

## 4. Deploy (DEPLOYMENT.md evidence)
```bash
docker compose up --build
curl -i -X PATCH http://localhost:8080/api/activities/bulk-status -H 'X-Staff-Id: s-3' -H 'Content-Type: application/json' -d '{...}'
```

## Files you must still produce from the real run
`spec.md`, `sprint-N-contract.md`, generator summaries, evaluator feedback, run-logs (all land in `.harness/reviews/`), `DEPLOYMENT.md` (with screenshot/URL),
`REFLECTION.md` (≤1 page, a *specific* limitation seen in your run). These cannot be pre-written: reviewers check that they are internally consistent with each other.
