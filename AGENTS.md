# Engineering guardrails

Every agent must, before changing this repository:

1. Read this file and `docs/KNOWN_FIXED_BUGS.md`.
2. Run `git status` and inspect the relevant diff.
3. Run the applicable baseline test/build.
4. Identify affected business flows and use the smallest safe patch.
5. Preserve existing behaviour and never reset, clean, or overwrite user changes.
6. Add or update a regression test when a defect is fixed.
7. Run focused tests plus applicable lint/build after the change.
8. Review the final diff and update bug and regression documentation.
9. Do not claim completion when a regression or failed verification remains.
