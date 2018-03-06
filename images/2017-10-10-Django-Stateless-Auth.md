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

```python
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
```python
jwt.encode(payload_to_be_signed, "myapplicationwidesecret", "HS512")
```

and the decoding method would look like
```python
jwt.decode(jwt_token, "myapplicationwidesecret", "HS512")
```

