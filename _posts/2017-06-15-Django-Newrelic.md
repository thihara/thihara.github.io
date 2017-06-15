---
layout: post
title: Setting up NewRelic on a Django app deployed on Apache using mod_wsgi
---

New Relic is normally pretty easy to integrate and doesn't require any code modifications - at least it didn't when I
was using their Java agent.

Deploying their python agent on my Django application deployed with `Apache HTTPD` and `mod_wsgi` didn't turn out to be
so straightforward.

You can't use their simple install process on the `Apache HTTPD` and `mod_wsgi` combination. You need to import and
initialize the agent in your code manually.

Let's see how that's going to work.

# Installing the library

Installation is pretty straight forward if you have `pip`,

```shell
pip install newrelic
```

or

```shell
sudo pip install newrelic
```

depending on your setup.

# Creating the configuration file

Once New Relic is installed through `pip` you can create the default configuration file with

```shell
newrelic-admin generate-config <new relic license key> newrelic.ini
```

This generates a basic `.ini` file with a bunch of properties that can be used to configure the details of New Relic.
You should have a look at the offered options in due time, however the one setting worth noting is the `app_name`
property.

```
app_name = <Your application name>
```

This is the name that will be displayed in the New Relic dashboard so change it to something that will help you identify
it quickly.

# Initializing the agent

To manually initialize the New Relic agent you need to import it in your code and then initialize it by providing the
path to the configuration file.

While New Relic stresses that this snippet must be placed before your other imports, it didn't matter if we load it a
 few lines down the file. For Django the place to do this is in the `wsgi.py` file.

```python
import newrelic.agent
newrelic.agent.initialize('/path/to/newrelic.ini')
```

After initialization of the agent we need to wrap our wsgi application object in New Relic's `WSGIApplicationWrapper`.
This will allow New Relic to provide us with additional instrumentation designed specifically for various frameworks (in
our case Django).

While New Relic automatically wraps some wsgi servers, that is not the case for `Apache HTTPD` and `mod_wsgi` combo.
A direct quote from their docs

> Note: A WSGI application only generally needs to be wrapped where no framework specific instrumentation is provided
> for the web framework being used, and the WSGI server being used also isn't being instrumented such that any
> WSGI application is automatically wrapped. The main WSGI server where the WSGI application is not automatically
> wrapped is Apache/mod_wsgi.

Here is how you wrap the wsgi application object.

```python
application = get_wsgi_application()

application = newrelic.agent.WSGIApplicationWrapper(application)
```

And Viola! You are done, exercise your application a bit and you should see data metrics coming in to the New Relic
dashboard.

# Improvements

Now that everything is running, it's time to do a few improvements to the code. In a real life application we will need
New Relic to be configurable, an option to enable disable it and an externally configurable configuration file path.

Let's see what that will look like in Django.

## settings.py

We will add two configurations to the `settings.py` file.

```python
# New Relic settings
ENABLE_NEW_RELIC = os.environ['ENABLE_NEW_RELIC'] in ['True','true']
NEW_RELIC_CONFIG_FILE = os.environ['NEW_RELIC_CONFIG_FILE']
#End of New Relic settings
```

As you can see we are retrieving two environment parameters,
1. `ENABLE_NEW_RELIC` - a boolean value to enable or disable New Relic initialization
2. `NEW_RELIC_CONFIG_FILE` - the path to the newrelic.ini configuration file.

## wsgi.py

Let's see how we can use these configurations in the `wsgi.py` file

```python
import os

from django.conf import settings
from django.core.wsgi import get_wsgi_application

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "one_face_in.settings")

application = get_wsgi_application()

if settings.ENABLE_NEW_RELIC:
    import newrelic.agent

    newrelic.agent.initialize(settings.NEW_RELIC_CONFIG_FILE)
    application = newrelic.agent.WSGIApplicationWrapper(application)
```

We have moved all the New Relic initialization code to the bottom of the file. The agent will only be initialized if
the settings have been set to do so, and the config file path is also loaded from the settings.

