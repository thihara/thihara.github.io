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

JWT is basically an encrypted piece of text that can contain any information we want.

We uses a secret key to encrypt a text token and pass it on to the authenticated user. The user will pass over the
encrypted (signed) token to authenticate the requests.

A more detailed introduction can be found in the [JWT web site](https://jwt.io/introduction/).

## Installing a Python JWT Library

Multiple JWT libraries has popped up for various languages, so integrating this with any web framework should be
pretty straightforward.

A list of libraries and their full capabilities have been compiled by the nice folks at `jwt.io` and can be found
[here](https://jwt.io/#libraries).

We will be using the [python-jose] library to secure our Django application, it has all the nice ticks ticked!

Add `python-jose` to your `requirements.txt` file or just install it directly by `pip install python-jose`.

## Using python-jose

There are only two things that you need from the a JWT library. First is encoding/encrypting the token. Second is
decrypting the token.