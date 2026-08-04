# Sentinq Frontend Demo

Copy `index.html`, `styles.css`, and `app.js` into:

```text
sentinq-platform/src/main/resources/static/
```

Restart Spring Boot and open:

```text
http://localhost:8080
```

Expected backend endpoints:

- `GET /api/system/health`
- `POST /api/shopping/orchestrate`

The Command Center keeps mandates and audit events in browser memory for the current page session.
