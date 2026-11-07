# Shopping

## Database

The application connects to MySQL at `localhost:3306` and uses the `Shopping`
database. The JDBC URL creates the database if it does not exist and the
configured MySQL account has permission to create databases. Alternatively,
run `create-database.sql` using a MySQL account with database-creation
permission.

Configure credentials with environment variables before starting the app:

- `DB_USERNAME` (defaults to `root`)
- `DB_PASSWORD` (defaults to an empty password)
- `DB_URL` (optional; overrides the default MySQL URL)

Hibernate creates or updates the `products` and `users` tables on startup.

## Authentication and authorization

The API uses stateless JWT access tokens. Set `JWT_SECRET` to a Base64-encoded
random key of at least 32 bytes before starting the application. For example,
in PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

`JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` optionally sets the access-token
lifetime; it defaults to 900 seconds. Do not use the test secret outside tests.

- `POST /api/auth/register` — create an account with `email` and `password`.
  Passwords must be 8–72 characters and at most 72 UTF-8 bytes; new accounts
  always receive the `USER` role. The password is stored as a BCrypt hash.
- `POST /api/auth/login` — authenticate with `email` and `password`; returns an
  access token and its lifetime. Send it on protected requests as
  `Authorization: Bearer <accessToken>`.

Product reads (`GET /api/products` and `GET /api/products/{id}`) are public.
Creating, updating, or deleting products requires the `ADMIN` role. To assign
the role manually, register the intended account and update its role using a
trusted database administrator:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

The account must log in again after its role changes to receive a token with
the new role. Never allow the public registration endpoint to select a role.

## Product API

Base path: `/api/products`

- `GET /api/products` — list products
- `GET /api/products/{id}` — get a product
- `POST /api/products` — create a product
- `PUT /api/products/{id}` — replace a product
- `DELETE /api/products/{id}` — delete a product

Create and update requests accept JSON with `name`, `description`, and `price`.
Name and price are required; price must be zero or greater. `description` is
optional, and `stock` is optional and defaults to `0` when omitted; stock must
be zero or greater when provided.

The Product module is organized under `com.BaoNinh.Shopping.modules.Product`:

- `controller` handles HTTP requests and responses.
- `service` contains Product operations and entity-to-response mapping.
- `repository` provides database access through Spring Data JPA.
- `entity` contains the persisted Product model.
- `dto` contains API request and response models.
- `exception` maps missing products to HTTP 404 responses.
