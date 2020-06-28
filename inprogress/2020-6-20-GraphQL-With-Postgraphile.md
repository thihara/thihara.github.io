---
layout: post
title: Creating an authenticated GraphQL API with PostGraphile
---

# Overview

In this blog post we will look at how to create a GraphQL API over our PostgreSQL database and how to secure it
granularly using PostGraphile and PostgreSQL's inbuild security mechanisms.

## GraphQL

GraphQL is a data query and manipulation language for APIs.

What that means is The API exposes a set of data models and provides a query language for the consumers. The consumers
decide on the granularity of data they want.

This means the API owners can just define the public dataset and forget about API maintenance, and the consumers can
define and extract only the data they are interested in.

You may also perform update operations called mutations through GraphQL as well, if you so wishes to.

While you do not need to be familiar with GraphQL to follow the content in this post, a good starting point for
 GraphQL can be found [here](https://graphql.org/learn/).

## PostGraphile

PostGraphile is a tool/framework written with NodeJS, that lets you create a GraphQL API on top your PostgreSQL
database instantly. It can detect your data tables and create a GraphQL API to access that data.

You can learn more about PostGraphile by browsing the documentation [here](https://www.graphile.org/postgraphile/introduction/)

### Installation

You will need to have [NodeJS](https://nodejs.org/en/) and/or [npx](https://www.npmjs.com/package/npx) installed.

There are two options available to install PostGraphile. One is to install it globally using node and then execute it.

`npm install -g postgraphile`

And then you can run it using this command.

`npx postgraphile -c postgres://user:pass@localhost/mydb --watch --enhance-graphiql --dynamic-json`

The other is to use npx and run it directly.

`npx postgraphile -c postgres://user:pass@localhost/mydb --watch --enhance-graphiql --dynamic-json`

You need to change the connection URL (-c option) to reflect the settings in your database.

If installation was successful you should see an output similar to this

```shell

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
    is_superuser boolean NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    email text NOT NULL,
    last_login bigint NOT NULL,
    is_active boolean NOT NULL,
    user_name text NOT NULL,
    CONSTRAINT core_user_email_key UNIQUE (email)
)
```

2. core_employee table containing extended employee details.
```sql
CREATE TABLE public.core_employee
(
    id bigserial PRIMARY KEY,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    family_name character NOT NULL,
    given_name character NOT NULL,
    join_date date,
    status character varying(1) NOT NULL,
    bio text NOT NULL,
    image_url character varying(200) NOT NULL,
    languages character varying(255) NOT NULL,
    teams character varying(255) NOT NULL,
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

SELECT * from core_user; --Error
SELECT id, email, user_name, password FROM core_user; --Error
SELECT id, email, user_name FROM core_user; --OK

```

Now let's grant the original SELECT grant to the EMPLOYEE_MINION role.

```sql
REVOKE SELECT(id, email, user_name) ON core_user FROM EMPLOYEE_MINION --Remove limited select grant
GRANT SELECT ON core_user TO EMPLOYEE_MINION; --Grant full select access
```

You can control column access to UPDATE operations as well. For more details see [here](https://www.postgresql.org/docs/10/sql-grant.html)

## PostgreSQL row level security

PostgreSql supports row level security since version 9.5. Row level security is enforced via policies and, as it's name
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
should be created when the user is created.

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



### Check roles

Use the following select to check if a given Role (user) has another role assigned to it

Here we are checking if empminion role has employee_admin role granted to id.
```
SELECT pg_has_role('empminion', 'employee_admin', 'MEMBER');
```

# Enable postgraphile security support

```
CREATE EXTENSION pgcrypto;
CREATE ROLE NO_ACCESS_ROLE;
```

```
update core_user set password = crypt('123',gen_salt('bf')) where email = 'thihara@favoritemedium.com';

create role thiharafm;
grant EMPLOYEE_MINION to thiharafm;
grant EMPLOYEE_ADMIN to thiharafm;
REVOKE EMPLOYEE_ADMIN from thiharafm;
REVOKE SELECT ON core_user from EMPLOYEE_MINION;

update core_user set user_name = 'thiharafm' where email = 'thihara@favoritemedium.com';


-- The core user password need to be text type to be used with the crypt function in the authenticate function we are using below
ALTER TABLE public.core_user
    ALTER COLUMN password TYPE text;

create type public.jwt_token as (
  role text,
  exp integer,
  user_id integer,
  is_admin boolean,
  username text
);

drop function authenticate(text, text);

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
      account.is_superuser,
      account.email
    )::public.jwt_token;
  else
    return null;
  end if;
end;
$$ language plpgsql strict security definer;

select authenticate('thihara@favoritemedium.com','123');
```

# Inherent Issues

Admin Users create new users and grant them roles. These operations need to be checked. For example create users can
only be done by an ADMIN user. This is easy enough to implement with GRANT CREATA or GRANT UPDATE only to the
admin roles.

However when we need more complicated functionality - for example, minion admins can only create other minions. Admins
can create all types of users including minion admin and admin type users.

```sql


set role empadmin;

select * from core_employee;

DROP POLICY emp_minions on core_employee;


ALTER TABLE core_user add constraint unique_username unique(user_name);

update core_user cu SET user_name = ce.given_name
from core_employee ce where cu.id=ce.user_id;

select * from core_user where user_name='Adel';


SELECT r.rolname,
  ARRAY(SELECT b.rolname
        FROM pg_catalog.pg_auth_members m
        JOIN pg_catalog.pg_roles b ON (m.roleid = b.oid)
        WHERE m.member = r.oid) as memberof
FROM pg_catalog.pg_roles r
WHERE r.rolname !~ '^pg_'
ORDER BY 1;
```

## Postgraphile

```
postgraphile \
  --jwt-token-identifier public.jwt_token \
  --jwt-secret thisisanabsolutelysecurejwttoken \
  -c postgres://thihara:@localhost/sixpaq \
  -s public \
  --default-role no_access_role
```


 query MyQuery {
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

 mutation {
   authenticate(input: {email: "thihara@favoritemedium.com", password: "123"}) {
     jwtToken
   }
 }


 # Create roles (users)

 Create our users. Admin user is empadmin. Minion user is empminion

 > CREATE USER is equivalent to CREATE ROLE except that CREATE USER assumes LOGIN by default, while CREATE ROLE does not.

 ```sql
 CREATE USER empadmin; // Same as CREATE ROLE empadmin LOGIN
 CREATE USER empminion; // Same as CREATE ROLE empminion LOGIN
 ```

 Create a role EMPLOYEE_ADMIN without LOGIN
 Create a role EMPLOYEE_MINION without LOGIN
 ```
 CREATE ROLE EMPLOYEE_ADMIN;
 CREATE ROLE EMPLOYEE_MINION;
 ```

 Grant the roles into our users
 ```sql
 GRANT EMPLOYEE_ADMIN TO empadmin;
 GRANT EMPLOYEE_MINION to emp_minion
 ```
