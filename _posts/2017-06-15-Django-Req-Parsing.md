---
layout: post
title: Parsing Unsupported Requests in Django
---

Django is a mature web framework for Python. It does it's job well, however anyone writing a RESTful API  in Django will
soon find that it lacks some of the basic request parsing you would expect from a web framework.

This post is written for Django 1.9 and 1.11.

# The problem

If you have used Django before the you know request parameter for `GET` and `POST` methods are parsed by Django
automatically and placed inside similarly named properties of the `request` object.

```python
class HelloController(View):
    def get(self, request):
        hello_param = request.GET["helloParam"]

    def post(self, request):
        hello_param = request.POST["helloParam"]
```

So far so good, now it's time to add a little bit of modification to your controller, and being a RESTful API, it's
time to utilize the `PUT` method.

```python
class HelloController(View):
    def put(self, request):
        hello_param = request.PUT["helloParam"]
```

You expect this code to work and run it. KaPUT, pun intended, it does not. You will see an error thrown because there's
no attribute named `PUT` in the `request` object.

Django only parses the request parameters for `GET` and `POST` HTTP methods. The Django team have some good(ish)
reasons for this, it's explained in
[this](https://groups.google.com/forum/#!msg/django-developers/dxI4qVzrBY4/m_9IiNk_p7UJ) post. You also do not get the
convenience of accessing any uploaded files via the `request.FILES` attribute in other HTTP methods like `PUT`.

#Solution

Before you go any furthur there are third-party frameworks like
[Django REST Framework](http://www.django-rest-framework.org/) or [Tasty Pie](https://django-tastypie.readthedocs.io/)
that does a decent job of tackling the REST API issues faced by Django. So long as you adhere to their conventions.

However if you don't want to use them for some reason, may be the project is an existing one with a big code-base you
don't want to refactor, maybe you don't want to follow these framework conventions, or maybe you are prototyping and
don't have time to learn the new frameworks right now, you can use the following solutions.

## First Iteration

In order to access our `PUT` parameters we need to create our own `QueryDict` object manually.

```python
put_params = QueryDict(request.body)
```

No big deal, just a single line of code. But it gets ugly pretty soon, you will have to pepper this all over your
views, and then there's the file issue. Django doesn't populate the convenient `FILES` attribute for `PUT` requests.
You have to go through a lot of hassle to gain access to the files sent in a `PUT` request. Basically you need to access
the `upload_handlers` of the `request` and read the file streams, then of course you need to parse the parameters
other than the files that are probably in the request. HASSLE! HASSLE! HASSLE!

Hmm... Maybe there's something in one of these `REST` frameworks that can help us.

Look at what we found in [Django Piston](https://github.com/mozilla/django-piston).

```python
def coerce_put_post(request):
    """
    The try/except abominiation here is due to a bug
    in mod_python. This should fix it.
    """
    if request.method == "PUT":
        # Bug fix: if _load_post_and_files has already been called, for
        # example by middleware accessing request.POST, the below code to
        # pretend the request is a POST instead of a PUT will be too late
        # to make a difference. Also calling _load_post_and_files will result
        # in the following exception:
        #   AttributeError: You cannot set the upload handlers after the upload has been processed.
        # The fix is to check for the presence of the _post field which is set
        # the first time _load_post_and_files is called (both by wsgi.py and
        # modpython.py). If it's set, the request has to be 'reset' to redo
        # the query value parsing in POST mode.
        if hasattr(request, '_post'):
            del request._post
            del request._files

        try:
            request.method = "POST"
            request._load_post_and_files()
            request.method = "PUT"
        except AttributeError:
            request.META['REQUEST_METHOD'] = 'POST'
            request._load_post_and_files()
            request.META['REQUEST_METHOD'] = 'PUT'

        request.PUT = request.POST
```

When you get past the error handling and workarounds (explained in the comment) in there, what this method does is
actually simple. It sets the `request.method` as `POST` temporarily and trigger the `request._load_post_and_files()`
method. This `_load_post_and_files()` method is where all the request parameter and file parsing happen in Django, for
the `POST` method.

Once we trick the `_load_post_and_files()` method into parsing all the request details for us, we then set the method
back to `PUT`.

Django also doesn't automatically parse `JSON` based on the `content-type` of the request.

You need to parse the `JSON` data manually like this

```python
json_params = json.loads(request.body)
```

Using this code every time we need to get something parsed is not ideal. It's annoying and looks unclean, not to mention
being an affront to the Gods of Clean Code.

## Middleware to the rescue

The solution for our dilemma is called Django middleware. In case you aren't already familiar with Django middlewares,
they are basically hooks that can attach themselves to the request and response processing chain the alter them.
Checkout the official documentation from [here](https://docs.djangoproject.com/en/1.11/topics/http/middleware/) if you
need more information on how they work.

All we need to do is put these snippets in a Django middleware and we will have easy access to our request data.

```python
import json

from django.http import HttpResponseBadRequest
from django.utils.deprecation import MiddlewareMixin


class PutParsingMiddleware(MiddlewareMixin):
    def process_request(self, request):
        if request.method == "PUT" and request.content_type != "application/json":
            if hasattr(request, '_post'):
                del request._post
                del request._files
            try:
                request.method = "POST"
                request._load_post_and_files()
                request.method = "PUT"
            except AttributeError as e:
                request.META['REQUEST_METHOD'] = 'POST'
                request._load_post_and_files()
                request.META['REQUEST_METHOD'] = 'PUT'

            request.PUT = request.POST


class JSONParsingMiddleware(MiddlewareMixin):
    def process_request(self, request):
        if (request.method == "PUT" or request.method == "POST") and request.content_type == "application/json":
            try:
                request.JSON = json.loads(request.body)
            except ValueError as ve:
                return HttpResponseBadRequest("unable to parse JSON data. Error : {0}".format(ve))
```

As you can see in the `PutParsingMiddleware` we parse the `PUT` method parameters so long as the `content_type` is not
`JSON`.

We do the `JSON` data parsing for `PUT` and `POST` requests in the `JSONParsingMiddleware` class and put the parsed data
in the `request.JSON` attribute. We also return a `HttpResponseBadRequest` response if the `JSON` data is unparseable.

We can now enjoy the fruits of our labour.

```python
class HelloController(View):
    def post(self, request):
        hello_param = request.JSON["helloParam"]

    def put(self, request):
        hello_param = request.PUT["helloParam"]
        hello_file = request.FILES["helloFile"]
```

 Much cleaner wouldn't you say?