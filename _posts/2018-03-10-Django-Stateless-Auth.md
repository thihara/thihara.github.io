---
layout: post
title: Securing an stateless Django Application
---

Django is a mature web framework for Python. Among it's many uses is creating REST APIs (or a standalong backend for
that matter). Django provides an authentication and authorization mechanism out of the box, but if you wish to make
your REST API or the backend stateless, you need to roll your own.

# Stateless Authentication Mechanism

A good stateless authentication mechanism is JSON Web Tokens also known as [JWT](https://jwt.io/). Let's look at JWT in
a bit more detail.

## JWT

JWT is basically a signed piece of text that can contain any information we want.

We uses a secret key to sign the text token and pass it on to the authenticated user. The user will pass over the
signed token to authenticate the requests.

The server will verify the token passed over by the user using the secret key.

A more detailed introduction can be found in the [JWT web site](https://jwt.io/introduction/).

## Installing a Python JWT Library

Multiple JWT libraries has popped up for various languages, so integrating this with any web framework should be
pretty straightforward.

A list of libraries and their full capabilities have been compiled by the nice folks at `jwt.io` and can be found
[here](https://jwt.io/#libraries).

We will be using the [python-jose] library to secure our Django application, it has all the nice ticks ticked!

Add `python-jose` to your `requirements.txt` file or just install it directly by `pip install python-jose`.

## Creating our token
We need to decide what we will put in our JWT. While we can put anything in our token, it's not advisable to do
so. We don't want to increase the size of our token unnecessarily.

### Claim
Whatever we are embedding inside the JWT is called a Claim. A claim is an statement about the subject(user).

What that means practically is that any attribute we place inside the JWT is a claim.

### Token Content
So what will be include in our token? There are a few recommended claims that we should include like the
`exp` claim.

The `exp` (expiration time) claim identifies the expiration time on or after which the JWT will be invalid. The value
should be a NumericDate value. That basically means **Seconds Since the Epoch**

Let's see what out basic token looks like.

```
{'user_id': user_id, 'exp': datetime.utcnow() + timedelta(days=5)}
```

We have the `user_id`, identifying the user this token is representing, and we have the token expiry.


## Using python-jose

There are only two things that you need from the a JWT library. First is encoding the token. Second is
decoding and verifying the token.

`python-jose` provides these functionality, along with a lot of other functionalities, you can read the documentation
from [here](http://python-jose.readthedocs.io/en/latest/index.html) to get familiar with all the other features of the
library.

Using the library is pretty simple, we can easily sign our token using the `encode` method, and verify it
using the `decode` method.

You need two parameters to use these methods, the first is the secret, this is what the signing algorithm will use.
Unless you have a very specific requirement,this is generally one per application.

Second parameter we need is the algorithm that will be used to sign our token, we will use the `HS512` algorithm to sign
our token.

HS512 is [HMAC](https://en.wikipedia.org/wiki/HMAC) using [SHA-2](https://en.wikipedia.org/wiki/SHA-2),
basically it's a message signing mechanism. It will generate a hash of our message(or JWT in this case) and any
tampering of the Token will yield a different hash. So when we check the hash against the altered message we will know
that the message has been tampered with and is no longer valid, because the hashes are different.

It's not possible to generate the proper hash for the tampered message without the secret key, we used to originally
sign it.

So the encoding method would look like
```
jwt.encode(payload_to_be_signed, "myapplicationwidesecret", "HS512")
```

and the decoding method would look like
```
jwt.decode(jwt_token, "myapplicationwidesecret", "HS512")
```

If the JWT is invalid the `decode` method would throw an JWTError.

## Writing our JWT Service.
Now that we know how to encode/sign a JWT and decode it, let's write our own JWT service.

```Python
import uuid
from datetime import datetime, timedelta

from django.conf import settings
from jose import jwt
from jose.constants import ALGORITHMS


class JWTService:
    JWT_SECRET = settings.JWT["JWT_SECRET"]
    JWT_EXP_DELTA_DAYS = settings.JWT["JWT_EXP_DELTA_DAYS"]

    @staticmethod
    def create_token(user_id):
        payload = {''user_id': user_id,  'exp': datetime.utcnow() + timedelta(days=JWTService.JWT_EXP_DELTA_DAYS)}

        token = jwt.encode(payload, JWTService.JWT_SECRET, ALGORITHMS.HS512)

        return token

    @staticmethod
    def verify_token(jwt_token):
        jwt_payload = jwt.decode(jwt_token, JWTService.JWT_SECRET, ALGORITHMS.HS512)

        return jwt_payload
```

We have externalizes all the configurations. The secret and the token expiry in days has been moved to the Django
settings. We have also used the `python-jose` constants to set the algorithm to `HS512`.

There are two methods in our `JWTService` class. The `create_token` method accepts a `user_id` parameter and creates
a JWT token for that given user ID. We will then return the JWT to the user who successfully authenticated themselves.

The `verify_token` method accepts a `jwt_token` parameter, and decodes the JWT. If the JWT is invalid it throws a
JWTError and the calling method needs to handle it like this.

```Python
    try:
        decoded_payload = jwt_service.verify_token(jwt_token)
    except JWTError as e:
        // Invalid JWT, throw another error, or return a 401 HTTP Response
```

## Invalidating JWT
We will have to invalidate the JWTs, individually (when user signs off from a device) or the whole lot at once (when
your security has been breached).

Now invalidating the whole lot of JWTs is pretty easy, all we have to do is change the secret key used to encode/decode
JWTs and all old JWTs become invalid.

It's less straightforward to invalidate individual JWTs. There are many ways to do this, but I've found the simplest
way is to keep a current token version in the user table (or anywhere you store user data).

This version will be incremented everytime we invalidate a token, the new incremented version will then be persisted
to the user table.

The current version of the token will be embedded withing the JWT, and upon verification, we will check the token
version in the JWT with the current user token in our user table. If they don't match we will reject the JWT and
designate it invalid.

So our final JWTService class will look like this.

```Python
import uuid
from datetime import datetime, timedelta

from django.conf import settings
from jose import jwt, JWTError
from jose.constants import ALGORITHMS

from user_service import UserService

class JWTService:
    JWT_SECRET = settings.JWT["JWT_SECRET"]
    JWT_EXP_DELTA_DAYS = settings.JWT["JWT_EXP_DELTA_DAYS"]

    @staticmethod
    def create_token(user_id, phone_number, email, current_token_version):
        payload = {'user_id': user_id, 'version': current_token_version,
                   'exp': datetime.utcnow() + timedelta(days=JWTService.JWT_EXP_DELTA_DAYS)}

        token = jwt.encode(payload, JWTService.JWT_SECRET, ALGORITHMS.HS512)

        return token

    @staticmethod
    def verify_token(jwt_token):
        jwt_payload = jwt.decode(jwt_token, JWTService.JWT_SECRET, ALGORITHMS.HS512)

        # The get_user_by_id method returns the user information by user_id, we then retrieves the token_version of
        # that user and compare it with the token version of the JWT.
        user_token_version = UserService.get_user_by_id(jwt_payload["user_id"]).token_version
        if user_token_version != jwt_payload["version"]:
            raise JWTError("Invalidated JWT token")

        return jwt_payload
```

The `create_token` method now accepts a `current_token_version` as a parameter, this is the current version of the
user. You can optionally increment this upon successful authentication, if the user should only be logged into a single
device at a time.

The user data retrieval is encapsulated inside the `UserService` and should be self explanatory.

## Creating a decorator
Everything is good so far, but this code isn't very usable in a web application where you have public
view and private views that only authenticated users can access.

Do you really want to pepper the code to, retrieve the JWT from a HTTP request and validate it, all over your
controllers? I don't!

Luckily we can write a python decorator to handle this tedious crosscutting concern. Now python decorators are out of
the scope of this article, but you can get a good grasp of them from [here](https://wiki.python.org/moin/PythonDecorators).

We want the decorator to do two things, one is to validate the JWT passed over in a Django request. The other is to
provide the authenticated user details to the calling method in an easily accessible way.

Let's see what our decorator look like.

```Python
import inspect
from functools import wraps

from django.http import *

from jwt_service import *

AUTHORIZATION_HEADER_NAME = "HTTP_AUTHORIZATION"
BEARER_METHOD_TEXT = "Bearer "

jwt_service = JWTService()


def need_jwt_verification(decorated_function, injectables=[]):
    @wraps(decorated_function)
    def decorator(*args, **kwargs):
        request = args[0]

        # Make sure that this decorator isn't used anywhere else by mistake.
        if not isinstance(request, HttpRequest):
            raise RuntimeError(
                "This decorator can only work with django view methods accepting a HTTPRequest as the first parameter")

        # Missing header means authentication failed.
        if AUTHORIZATION_HEADER_NAME not in request.META:
            return HttpResponse("Missing authentication header", status=401)

        # Get the header content and strip out the "Bearer" characters.
        jwt_token = request.META[AUTHORIZATION_HEADER_NAME].replace(BEARER_METHOD_TEXT, "")

        try:
            decoded_payload = jwt_service.verify_token(jwt_token)
            user_id = decoded_payload["user_id"]

            # Check if the method this decorator is used on has parameters that should be populated wit the user_id
            parameter_names = inspect.getargspec(decorated_function).args

            if "user_id" in parameter_names:
                kwargs["user_id"] = user_id

            # Populate data for methods using request.user.id to get user_id value.
            user = type('', (), {})()
            user.id = user_id
            request.__setattr__("user", user)

            return decorated_function(*args, **kwargs)
        except JWTError as e:
            return HttpResponse("Incorrect or expired authentication header", status=401)

    return decorator
```

You can use this decoator on the view functions you want to protect.

In a class based view, you can use it via the `method_decorator` decorator (a bit strange, I know!).

```Python
import logging

from django.http import JsonResponse
from django.utils.decorators import method_decorator
from django.views.generic import View

from security_decorators import need_jwt_verification

logger = logging.getLogger(__name__)


@method_decorator([need_jwt_verification], name="dispatch")
class HelloController(View):
    def get(self, request, user_id):
        return JsonResponse('{"message":"Hello user with ID %s"}' % str(user_id), safe=False)
```

In a normal view function you can use it directly
```Python
from django.http import *
from django.views.decorators.http import require_GET

from security_decorators import need_jwt_verification

@require_GET
@need_jwt_verification
@csrf_exempt
def get_event_users(request):
    return JsonResponse('{"message":"Hello user with ID %s"}' % str(request.user.id), safe=False)
```

Note that anywhere we use the `user_id` method parameter we can use the request attribute `request.user.id` and
vice versa because we populate both in our decorator.

### Authorization Header & Bearer Token
This decorator expect the JWT to passed on as a [Bearer token](https://tools.ietf.org/html/rfc6750).

Basically this means you need to use the `Authorization` header to pass the word `Bearer` and then the JWT.

`Authorization : Bearer our.jwt.token`

The Django key for getting the Authorization HTTP header is `HTTP_AUTHORIZATION`.

### JWT Content Access
The decorator should be pretty explanatory with all the comments in there, one thing worth repeating is the parameter
population, this decorator will automatically populated view parameters named `user_id` on the views it is applied on.

And any view that doesn't want to declare method parameters can access the `user_id` via the request attributes
`request.user.id`.

This is a very convenient way for the views to access any claims we emdbed in our JWT.

## Versions

I used `Python 2.7.9`, `Django 1.11` and `python-jose 1.3.2`. However I don't imagine things being much different in
Python 3+ and newer versions of the libraries, so this code should still work as intended.