## What If Food - Analytics

## Env vars

Create dev-config.edn (or use dev-config.sample.edn). Add required env vars
\*Env vars are run against a malli spec. Make sure to update `wif-analytics.specs.env` when changes env vars

```bash
# Used to access the wif-analytics endpoints
export API_KEY=secret

# Postgres details
export DB__NAME=postgres
export DB__USER=postgres
export DB__PASSWORD=postgres
export DB__HOST=localhost
export DB__PORT=5432

# Local dev settings
export DEV=true
export PORT=3000
export NREPL_PORT=7000
```

## Start docker compose

1. This will start postgres

```bash
docker-compose up
```

## Restore DB

1. With DB named postgres
1. With username postgres

```bash
psql -U postgres -h localhost -p 5432 postgres < dump.sql
```

Enter in password `postgres`

## API docs

http://localhost:3000/api/api-docs/index.html
