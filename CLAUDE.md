# Overview

This is a spring boot backend for the more system. This part creates, modifies and manages studies. It has multiple
modules, for better coupling, and all modules are exported into their own MVN packages, which are used by the
BlendedCare Backend. In future also by the data gateway.
The frontend is the Studymanager Frontend and authentication is given by keycloak.
Always test your changes. Always run all tests at the end of your development cycle, to check for any errors.
Never invent classes, methods or frameworks that do not exist.
Never commit or push, it is critical, that the code is reviewed by a human.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:

- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use
  `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a
  scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough
  context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
