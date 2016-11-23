---
layout: post
title: Creating Your Own E-Mail service with Haraka, PostgreSQL and AWS S3
---

I'm sure anyone who will be reading this post online would've used E-Mail. It's a pretty convenient method of
communicating and there are a lot of free email service providers out there.

And if you are a developer like myself, you have a lot of paid email services out there that offers various integration
options and features. However most of the time they aren't a 100% customizable to one's requirements. Even setting up
your own email server doens't really allow you to customize every aspect of the email pipeline.

But if you are a developer like me, we can use Haraka. It's an E-Mail server written in Node.js that has a plugin
based architecture. What that means is, we can pretty much do whatever we want with Haraka by writing plugins.
All you need is a little bit of javascript knowledge. If you already don't have it, just acquire it. Javascript is a
pretty easy language to learn, and you don't need a lot of advanced skills to write a Haraka plugins.

In this post, we will be exploring how we can setup Haraka to receive and send emails. How to write a plugin to validate
recipients and accept or reject the email. How to write a plugin to place incoming emails in an Amazon AWS bucket.

It's going to be a long post!

##Installing up Haraka

You will first need to have [npm](https://www.npmjs.com/) and [Node.js](https://nodejs.org/en/) installed in your
system. The installation instructions are pretty clear and straightforward.

Once node and npm setup is completed all you have to do is install Haraka by typing in

```
npm install -g Haraka
```

The `-g` flag is for installing Haraka globally. You might have to provide root privileges on a unix based system.

Once Haraka is installed, you need to create a configuration for it. Without a configuration you can't do anything.

```
haraka -i /path/to/haraka_test
```

This will create a directory named `haraka_test` that has all the configurations necessary to run a Haraka server
instance.

The `haraka_test` contains the following directories and files.

* config
    - A directory containing various configuration files.
* docs
    - A directory containing documentation.
* plugins
    - A directory containing the custom plugins.
* package.json
    - A file containing additional node dependencies that the plugins need to run.

One important file to note right now is the `config/smtp.ini` file. You can change server listening details like the
port, ip etc. from that file. The default port for Haraka is `25`, which requires root/admin privileges depending on the
system you are using.

Now that's just an overview of whats created when you create the initial configuration. We will go into more details
as we proceed down this post.

So let's start her up.

```
sudo Haraka -c /path/to/haraka_test/
```

You will see some log output on the console, and then the server should start up. While there's nothing much you can do
right now the way this server is setup, it's the first step!

##Picking up incoming E-Mails

After the server is installed successfully, it's time to to set it up to pick up incoming emails. So here's the thing
to do this properly, you will need your own domain.

###MX Record

Now everyone knows, or I hope they do, what a DNS record is, basically that's what tells everyone which domain is
pointed at which server.

There's something called a `MX` record as well. That's shorthand for `Mail Exchange`. Now this record tells anyone who
wants to check which `Mail Exchange Server(s)` are associated with a domain.

I got `thihara.com` from GoDaddy, and you can set your `MX` record from their DNS management screen. It basically looks
like this.

![MX Record]({{ site.baseurl }}/images/haraka/mx_record.png)

Now, you want this to point to the address of the server you are running the Haraka server from. Note that I have the MX
record pointed at the DNS of my AWS EC2 instance. You can't point it to an IP address directly, at least not with 
GoDaddy.
