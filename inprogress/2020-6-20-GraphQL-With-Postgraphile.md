---
layout: post
title: Creating an authenticated GraphQL API with PostGraphile
---

# Overview

## Graph QL

Graph QL is a way a data model can be exposed to a consumer using a minimal API. What that means is

The API exposes a set of data models and provides a query language for the consumers. The consumers decide on the
granularity of data they want.

### Installation


# PostgreSQL Row Level Security

```sql
ALTER TABLE core_employeedetail ENABLE ROW LEVEL SECURITY;
```

# Create roles (users)

> CREATE USER is equivalent to CREATE ROLE except that CREATE USER assumes LOGIN by default, while CREATE ROLE does not.

```sql
CREATE USER empadmin; // Same as CREATE ROLE empadmin LOGIN
CREATE USER empminion; // Same as CREATE ROLE empminion LOGIN
```

Create a role EMPLOYEE_ADMIN without LOGIN
Create a role EMPLOYEE_MINION without LOGIN

Create user admin
Create user minion
```sql
GRANT EMPLOYEE_ADMIN TO admin;
GRANT EMPLOYEE_MINION to minion
```

# Postgresql Policies


# Inherent Issues

Admin Users create new users and grant them roles. These operations need to be checked. For example create users can
only be done by an ADMIN user. This is easy enough to implement with GRANT CREATA or GRANT UPDATE only to the
admin roles.

However when we need more complicated functionality - for example, minion admins can only create other minions. Admins
can create all types of users including minion admin and admin type users.

```sql
select * from core_employee;
alter table core_employee enable row level security;
create user empadmin;
create user empminion;
GRANT SELECT ON core_employee TO EMPLOYEE_ADMIN;
GRANT SELECT ON core_user TO EMPLOYEE_ADMIN;

GRANT SELECT ON core_employee TO EMPLOYEE_MINION;
GRANT SELECT ON core_user TO EMPLOYEE_MINION;

CREATE ROLE EMPLOYEE_ADMIN; GRANT EMPLOYEE_ADMIN TO empadmin;
CREATE ROLE EMPLOYEE_MINION; GRANT EMPLOYEE_MINION TO empminion;

select * from core_employeedetail;
alter table core_employeedetail enable row level security;

CREATE POLICY emp_minions ON core_employee TO EMPLOYEE_MINION
USING (EXISTS (SELECT user_name FROM core_user WHERE id = user_id and user_name = current_user));

CREATE POLICY emp_admin ON core_employee TO EMPLOYEE_ADMIN
USING (true);

set role empadmin;

select * from core_employee;

DROP POLICY emp_minions on core_employee;


ALTER TABLE core_user add constraint unique_username unique(user_name);

update core_user cu SET user_name = ce.given_name
from core_employee ce where cu.id=ce.user_id;

select * from core_user where user_name='Adel';

SELECT pg_has_role('empminion', 'employee_admin', 'MEMBER');

SELECT r.rolname,
  ARRAY(SELECT b.rolname
        FROM pg_catalog.pg_auth_members m
        JOIN pg_catalog.pg_roles b ON (m.roleid = b.oid)
        WHERE m.member = r.oid) as memberof
FROM pg_catalog.pg_roles r
WHERE r.rolname !~ '^pg_'
ORDER BY 1;
```