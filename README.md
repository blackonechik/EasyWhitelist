## Easy Whitelist

Paper plugin that checks whitelist entries from PostgreSQL instead of `whitelist.json`.

### What it does

- Blocks players whose nickname is not present in the database.
- Uses a single JDBC URL in `config.yml`.
- Keeps an in-memory cache and refreshes it on a timer.
- Includes admin commands to reload, add, remove, and list whitelist entries.

### Configuration

Set the PostgreSQL connection in `config.yml`:

```yaml
database:
  jdbc-url: jdbc:postgresql://127.0.0.1:5432/minecraft?sslmode=disable
```

Put the database host, port, database name, user, password, and SSL options into the JDBC URL itself if you need them.

### Commands

- `/easywhitelist reload`
- `/easywhitelist add <nick>`
- `/easywhitelist remove <nick>`
- `/easywhitelist list`

### Permissions

- `easywhitelist.admin.reload`
- `easywhitelist.admin.add`
- `easywhitelist.admin.remove`
- `easywhitelist.admin.list`

The parent permission `easywhitelist.admin` is also available and grants all sub-permissions.

### Database table

The plugin creates the table automatically if it does not exist. The current schema is:

```sql
CREATE TABLE IF NOT EXISTS whitelist_entries (
    nickname VARCHAR(16) PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### For your website

Your backend can write directly to `whitelist_entries` by inserting or updating `nickname` and `active`.

[SQL example](sql/whitelist.sql)
 
