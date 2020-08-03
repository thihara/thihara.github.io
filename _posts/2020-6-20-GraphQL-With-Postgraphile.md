---
layout: post
title: Creating an authenticated GraphQL API with PostGraphile
---

# Overview
In this blog post we will look at how to create a GraphQL API over our PostgreSQL database and how to secure it
granularly using PostGraphile and PostgreSQL's inbuilt security mechanisms.

## GraphQL
GraphQL is a data query and manipulation language for APIs.

What that means is that the API exposes a set of data models and provides a query language for the consumers.
The consumers decide on the granularity of data they want.

> A consumer is any user of the API, whether internal (application front end) or external (a third party application)

This means the API owners can just define the public dataset and forget about API maintenance, and the consumers can
define and extract only the data they are interested in.

You may also perform update operations called mutations through GraphQL as well, if you so wish to.

For example you can have a table named `core_employee` containing employment related data like family name and
given name.

If you define a GraphQL API for this table (or object, Employee) through PostGraphile you no longer need to write multiple
fine grained APIs for this table for data retrieval. The consumer can decide what the API should return by specifying that in
the query.

The following query specify that only the id (the primary key), and the family_name column is to be returned

```graphql
query MyQuery {
  allCoreEmployees {
    edges {
      node {
        id
        familyName
      }
    }
  }
}
```

The result would be similar to

```json
{
  "data": {
    "allCoreEmployees": {
      "edges": [
        {
          "node": {
            "id": 1,
            "familyName": "Jayathunga"
          }
        },
        {
          "node": {
            "id": 71,
            "familyName": "Test"
          }
        },
        {
          "node": {
            "id": 72,
            "familyName": "Nik"
          }
        }
      ]
    }
  }
}
```

Another query that request only the id column of the employee looks like this

```graphql
query MyQuery {
  allCoreEmployees {
    edges {
      node {
        id
      }
    }
  }
}
```

And the result would be
```json
{
  "data": {
    "allCoreEmployees": {
      "edges": [
        {
          "node": {
            "id": 1
          }
        },
        {
          "node": {
            "id": 71
          }
        },
        {
          "node": {
            "id": 72
          }
        }
      ]
    }
  }
}
```

The key point here is that both these requests go to the same API endpoint, yet the output is different based on what
the query requests.


While you do not need to be familiar with GraphQL to follow the content in this post, a good starting point for
 GraphQL can be found [here](https://graphql.org/learn/).

## PostGraphile
PostGraphile is a tool/framework written with NodeJS, that lets you create a GraphQL API on top of your PostgreSQL
database instantly. It can detect your data tables and create a GraphQL API to access that data.

You can learn more about PostGraphile by browsing the documentation [here](https://www.graphile.org/postgraphile/introduction/)

### Installation
You will need to have [NodeJS](https://nodejs.org/en/) and/or [npx](https://www.npmjs.com/package/npx) installed.

There are two options available to install PostGraphile. One is to install it globally using node and then execute it.

`npm install -g postgraphile`

And then you can run it using this command.

`npx postgraphile -c postgres://user:password@localhost/mydb --watch --enhance-graphiql --dynamic-json`

The other is to use npx and run it directly.

`npx postgraphile -c postgres://user:password@localhost/mydb --watch --enhance-graphiql --dynamic-json`

You need to change the connection URL (-c option) to reflect the settings in your database.

If installation was successful you should see an output similar to this

```shell
PostGraphile v4.7.0 server listening on port 5000

  ‣ GraphQL API:         http://localhost:5000/graphql
  ‣ GraphiQL GUI/IDE:    http://localhost:5000/graphiql (enhance with '--enhance-graphiql')
  ‣ Postgres connection: postgres://thihara@localhost/sixpaq
  ‣ Postgres schema(s):  public
  ‣ Documentation:       https://graphile.org/postgraphile/introduction/
  ‣ Join Nigel Taylor in supporting PostGraphile development: https://graphile.org/sponsor/
```

Note that PostGraphile can also be used as a library from inside your existing application, see documentation
 [here](https://www.graphile.org/postgraphile/usage-library/) for more details.

# PostgreSQL Security
Setting up an API over you database is useful, but you can't really use it in general applications unless you can
secure it.

PostGraphile provides JWT support that can be combined with PostgreSQL security features to provide an authentication
and authorization framework.

Let's look at some of the security mechanisms we can use to secure our data at the PostgreSQL database level.

## Table and role setup
We will use two tables for illustration purposes.

1. core_user table containing core user data like email, username and password.

```sql
CREATE TABLE public.core_user
(
    id bigserial PRIMARY KEY,
    password text COLLATE pg_catalog."default",
    email text NOT NULL,
    user_name text NOT NULL,
    CONSTRAINT core_user_email_key UNIQUE (email)
)
```

2. core_employee table containing extended employee details.
```sql
CREATE TABLE public.core_employee
(
    id bigserial PRIMARY KEY,
    family_name text NOT NULL,
    given_name text NOT NULL,
    user_id integer REFERENCES core_user(id),
    CONSTRAINT core_employee_user_id_key UNIQUE (user_id)
);
```

Our aim is to create two roles.

1. EMPLOYEE_ADMIN - Have access to all rows of the table
```sql
CREATE ROLE EMPLOYEE_ADMIN;
```

2. EMPLOYEE_MINION - Have access only to data related to their own user
```sql
CREATE ROLE EMPLOYEE_MINION;
```

## PostgreSQL table and column level security
Now that we created our two roles, we need to grant them permission into tables. We can grant table level permissions
into each operation like SELECT, UPDATE, DELETE.

Grant the select permissions for both roles

```sql
GRANT SELECT ON core_employee TO EMPLOYEE_ADMIN;
GRANT SELECT ON core_user TO EMPLOYEE_ADMIN;

GRANT SELECT ON core_employee TO EMPLOYEE_MINION;
GRANT SELECT ON core_user TO EMPLOYEE_MINION;
```

We can further restrict them by columns. The following snippet will remove the initial SELECT permission (grant) from
EMPLOYEE_MINION and limit SELECT operations for only the id, email and user_name columns.

```
REVOKE SELECT ON core_user FROM EMPLOYEE_MINION --Remove initial grant

GRANT SELECT(id, email, user_name) ON core_user to EMPLOYEE_MINION; --Grant permission only for the required columns

SET ROLE employee_minon; --Set the employee_minion role as the current role
SELECT * from core_user; --Error
SELECT id, email, user_name, password FROM core_user; --Error
SELECT id, email, user_name FROM core_user; --OK

```

Now let's grant the original SELECT grant to the EMPLOYEE_MINION role.

```sql
REVOKE SELECT(id, email, user_name) ON core_user FROM EMPLOYEE_MINION --Remove limited select grant
GRANT SELECT ON core_user TO EMPLOYEE_MINION; --Grant full select access

SET ROLE thihara; --thihara is the database owner (admin) in my local database and we are reverting to that user
```

You can control column access to UPDATE operations as well. For more details see [here](https://www.postgresql.org/docs/10/sql-grant.html)

## PostgreSQL row level security
PostgreSQL supports row level security since version 9.5. Row level security is enforced via policies and, as it's name
implies, allows us to control access to individual database rows. And it needs to be turned on for individual
tables before it will be enforced.

Let's enable row level security for our two tables
```sql
ALTER TABLE core_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE core_user ENABLE ROW LEVEL SECURITY;
```

Let's create a policy to grant access to the EMPLOYEE_MINION role to only see rows from the core_employee table when the
associated username is the current user. Note the username is stored in the core_user table and the core_employee
only have the user_id column referencing the core_user table, hence the inner query with the EXISTS clause.

> Note that the user_name column contains the PostgreSQL user name (or the role name in PostgreSql terms). This
PostgreSQL role should be created when the user is created.

```
CREATE POLICY emp_minions ON core_employee TO EMPLOYEE_MINION
USING (EXISTS (SELECT user_name FROM core_user WHERE id = user_id and user_name = current_user));
```

If we want to only enable select oeprations we can change the policy to this.
```sql
CREATE POLICY emp_minions ON core_employee FOR SELECT TO EMPLOYEE_MINION
USING (EXISTS (SELECT user_name FROM core_user WHERE id = user_id and user_name = current_user));
```

Now we grant the EMPLOYEE_ADMIN user access to all the data in the core_employee table.
```
CREATE POLICY emp_admin ON core_employee TO EMPLOYEE_ADMIN
USING (true);
```

If we only want to enable select operation we can change the policy like before.
```
CREATE POLICY emp_admin ON core_employee FOR SELECT TO EMPLOYEE_ADMIN
USING (true);
```

Let's create similar roles for the core_user table
```
CREATE POLICY user_minions ON core_user TO EMPLOYEE_MINION
USING (user_name = current_user);

CREATE POLICY user_admin ON core_user TO EMPLOYEE_ADMIN
USING (true);
```

While we don't use it in our policies the following query can be used to check if a given Role (user) has
another role assigned to it.

Here we are checking if empminion role has EMPLOYEE_ADMIN role granted to id.
```
SELECT pg_has_role('empminion', 'employee_admin', 'MEMBER');
```

> Note: PostgreSQL users are roles. In the above query we are checking if a role (or a user) named empminion has the
role EMPLOYEE_ADMIN assigned to it. A role can have multiple other roles assigned (granted) to it.

For more information on policies see [here](https://www.postgresql.org/docs/10/ddl-rowsecurity.html)

Now let's test out our newly minted roles!

First let's add two accounts and related employee records, feel free to seed the tables with more data if you so desire.
```sql
-- Insert admin user
INSERT INTO public.core_user(id, password, email, user_name) VALUES(1, '123', 'thihara@favoritemedium.com', 'adminthihara');
INSERT INTO public.core_employee(id, family_name, given_name, user_id) VALUES (1, 'Admin', 'Thihara', 1);

--Insert minion user
INSERT INTO public.core_user(id, password, email, user_name) VALUES(2, '123', 'thihara+minion@favoritemedium.com', 'minionthihara');
INSERT INTO public.core_employee(id, family_name, given_name, user_id) VALUES (2, 'Minion', 'Thihara', 2);

--Now create their roles
CREATE ROLE adminthihara;
CREATE ROLE minionthihara;

--Now grant their roles
GRANT EMPLOYEE_ADMIN TO adminthihara;
GRANT EMPLOYEE_MINION to minionthihara;
```

Time to see how well our configuration works!
```sql
SET ROLE minionthihara; --Set the current role to be employee_minion (minionthihara has that role)
SELECT * FROM core_user; --Only return the row that belongs to minionthihara
SELECT * FROM core_employee; --Only return the row that belongs to minionthihara

SET ROLE adminthihara; --Set the current role to be employee_admin (adminthihara has that role)
SELECT * FROM core_user; --Return all rows
SELECT * FROM core_employee; --Return all rows

SET ROLE thihara; --thihara is the database owner (admin) in my local database and we are reverting to that user
```

# PostGraphile security support
Now that we created the roles and setup our policies in PostgreSQL, let's see what's needed to configure PostGraphile
to work with our database.

First we need to add the `pgcrypto` extension to PosrgreSQL. We will use it's crypt method to hash our passwords.
```sql
CREATE EXTENSION pgcrypto;
```

Now that we enabled the extension let's update the user passwords.
```sql
UPDATE core_user SET password = crypt('123', gen_salt('bf')) WHERE email = 'thihara@favoritemedium.com'; --admin user
UPDATE core_user SET password = crypt('456', gen_salt('bf')) WHERE email = 'thihara+minion@favoritemedium.com'; --minion user
```

Then we need to create a role with no access to be used by default if a user isn't authenticated. We will be providing
this to postgraphile at startup.
```sql
CREATE ROLE NO_ACCESS_ROLE;
```

Next we need to create a JWT type in PostgreSQL. Ignore the double token in the
type (JWT = JSON web token, jwt_token = JSON web token token), it's intended to be more readable than jwt. This type
will contain the information we want to embed in the JWT. This will also be provided to postgraphile at startup.

```sql
CREATE TYPE public.jwt_token as (
  role text, --db role of the user
  exp integer, --expiry date as the unix epoch
  user_id integer --db identifier of the user,
  username text --username used to sign in, user's email in our case
);
```

Now let's create the authenticate function that we will use to authenticate users.
```
create function public.authenticate(
  email text,
  password text
) returns public.jwt_token as $$
declare
  account public.core_user;
begin
  select a.* into account
    from public.core_user as a
    where a.email = authenticate.email;

  if account.password = crypt(password, account.password) then
    return (
      account.user_name,
      extract(epoch from now() + interval '7 days'),
      account.id,
      account.email
    )::public.jwt_token;
  else
    return null;
  end if;
end;
$$ language plpgsql strict security definer;

SELECT authenticate('thihara@favoritemedium.com','123'); --Test it out
```

This function will return the JWT upon successful authentication or null if authentication failed.

## Starting PostGraphile
Now that all the configuration is done let's start the PostGraphile server.
```
postgraphile \
  --jwt-token-identifier public.jwt_token \
  --jwt-secret thisisanabsolutelysecurejwttoken \
  -c postgres://thihara:@localhost/sixpaq \
  -s public \
  --default-role no_access_role
```

See how the `jwt_token`, and `no_access_role` is passed to PostGraphile at startup.

Make sure the `--jwt-secret` is passed a proper secret instead of my dummy secret value.

Now let's authenticate using our authenticate function. In GraphQL terms this is a mutation. Send a post request to
the PostGraphile API endpoint.

The default endpoint is `http://localhost:5000/graphql`

```graphql
mutation {
    authenticate(input: {email: "thihara@favoritemedium.com", password: "123"}) {
        jwtToken
    }
}
```

Here's the cURL command

```shell
curl --location --request POST 'http://localhost:5000/graphql' \
--header 'Content-Type: application/json' \
--data-raw '{"query":"mutation {\n  authenticate(input: {email: \"thihara@favoritemedium.com\", password: \"123\"}) {\n    jwtToken\n  }\n}","variables":{}}'
```

You will receive your JWT if authentication is successful.

You can now run the queries. Remember the JWT must be sent as a Bearer Token.

```graphql
query {
  allCoreUsers {
    edges {
      node {
        id
        email
        userName
        password
      }
    }
  }
}
```

Here's the cURL command for the query.

```shell
curl --location --request POST 'http://localhost:5000/graphql' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoidGhpaGFyYWZtIiwiZXhwIjoxNTkzODYxNTkxLCJwZXJzb25faWQiOjM4LCJpc19hZG1pbiI6ZmFsc2UsInVzZXJuYW1lIjoidGhpaGFyYUBmYXZvcml0ZW1lZGl1bS5jb20iLCJpYXQiOjE1OTMyNTY3OTAsImF1ZCI6InBvc3RncmFwaGlsZSIsImlzcyI6InBvc3RncmFwaGlsZSJ9.fA7qZevBJwt4OiOo3O59EpJjxA_4hZWwPZVDqzimen8' \
--header 'Content-Type: application/json' \
--data-raw '{"query":"query {\n  allCoreUsers {\n    edges {\n      node {\n        id\n        email\n        userName\n        password\n      }\n    }\n  }\n}","variables":{}}'
```

PostGraphile decodes the passed JWT and uses it to set the current role before executing the SQL queries in the
database. This is effectively similar to how we used the `SET ROLE` command to change the current role.

You can test out the core_employee table by crafting a query for that as well. Consider it a useful exercise!

Another useful exercise would be to write a register function to create a new user and a new PostgreSQL role for
the user. Don't forget to hash the password!

Please read the PostGraphile security section from [here](https://www.graphile.org/postgraphile/security/) to learn
more.

# Conclusion

A tool like PostGraphile is to help non technical stakeholders understand the data they are working
with.

An example would be a UI/UX designer working on revamping a legacy application. PostGraphile would be a very
useful tool for the designer to get a feel for the data and it's structure/relationship. It would be easier to
understand than something like SQL because it's basically a object hierarchy, which for me at least, is easier to
digest.

Same goes for a QA engineer trying to analyze the data available in the database and a project manager trying to
understand a data issue (or just data).

In conclusion PostGraphile could be a useful tool for rapid prototyping, and early stage application development. With
addition of role based authentication and row level security you can add robust access control into your database and
your GraphQL API. But as your application goes beyond that you are likely to need a proper backend for your application.

