---
layout: post
title: Creating Your Own E-Mail service with Haraka, PostgreSQL and AWS S3
---

I'm sure anyone who will be reading this post online would've used E-Mail. It's a pretty convenient method of
communicating and there are a lot of free email service providers out there.

And if you are a developer like myself, you have a lot of paid email services out there that offers various integration
options and features. However most of the time they aren't a 100% customizable to one's requirements. Even setting up
your own email server doens't really allow you to customize every aspect of the email pipeline.

But if you are a developer like me, we can use Haraka. It's an SMTP server written in Node.js that has a plugin
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

##Receiving Emails

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

You want this to point to the address of the server you are running the Haraka server from. Note that I have the MX
record pointed at the DNS of my AWS EC2 instance. You can't point it to an IP address directly, at least not with
GoDaddy.

###SMTP

It's worth noting that the responsibility of an SMTP server like Haraka is two fold.

1. Receiving and accepting emails and then forwarding them to their destinations.
2. Sending outgoing emails.

In no way will the SMTP server itself store the emails, nor does it have any concept of an inbox/mailbox (Where
emails specific to a single user will be stored).

Storing emails and maintaining inboxes is generally done by separate servers like IMAP or POP3. We will be


###Setting up a Queue

A queue is a term Haraka uses to describe how the email is forwarded. The queue is going to decide what will happen to
the emails that are received by the server. You can decide to use a queue that will discard the received emails for
testng purposes, or use a plugin that will forward the emails to a POP3 or an IMAP server. Even put the emails in
an S3 bucket or in some kind of a database.

###Swaks

Swaks is a script that you can use to test the SMTP server by sending emails. It can be found in
[http://www.jetmore.org/john/code/swaks/](http://www.jetmore.org/john/code/swaks/)

Once downloaded and installed the script can be used to test the email receiving functionality on a locally setup
Haraka server like this

```
swaks -s localhost -t thihara@thihara.com -f thihara1@thihara.com
```

Or to test your DNS and server associations simply use

```
swaks -t thihara@thihara.com -f thihara1@thihara.com
```

You will see an output similar to

```
=== Trying localhost:25...
=== Connected to localhost.
<-  220 Thiharas-MacBook-Pro.local ESMTP Haraka 2.8.8 ready
 -> EHLO thiharas-macbook-pro.local
<-  250-Thiharas-MacBook-Pro.local Hello Unknown [127.0.0.1], Haraka is at your service.
<-  250-PIPELINING
<-  250-8BITMIME
<-  250-SIZE 0
<-  250-STARTTLS
<-  250 AUTH PLAIN LOGIN CRAM-MD5
 -> MAIL FROM:<thihara1@thihara.com>
<-  250 sender <thihara1@thihara.com> OK
 -> RCPT TO:<thihara@thihara.com>
<-  250 recipient <thihara@thihara.com> OK
 -> DATA
<-  354 go ahead, make my day
 -> Date: Mon, 28 Nov 2016 09:46:46 +0530
 -> To: thihara@thihara.com
 -> From: thihara1@thihara.com
 -> Subject: test Mon, 28 Nov 2016 09:46:46 +0530
 -> X-Mailer: swaks v20130209.0 jetmore.org/john/code/swaks/
 ->
 -> This is a test mailing
 ->
 -> .
<** 451  (919BCDD3-2819-44C5-9E48-CE0AAFD2ABF7.1)
 -> QUIT
<-  221 Thiharas-MacBook-Pro.local closing connection. Have a jolly good day.
=== Connection closed with remote host.
```

You will notice that the final response code is `451`. This is because we haven't really setup a queue yet. We will get
to that down the line!

Of course the localhost will be replaced with `thihara.com` or whatever is the recipient domain if you were testing
against an actual DNS baked email server.

You can see what other syntax is supported by the tool form their website. It basically support everything related to
emails, including attachments, embedded html etc.

## Writing the S3 queue plugin

Now our email server is setup to receive emails, let see how we can write a Haraka queue plugin to push the received
emails to an AWS S3 bucket.

You need to add the AWS dependencies since our plugin is going to need to connect to an AWS S3 bucket. So go to the
`/path/to/haraka_test` directory and type in

```
npm install --save aws-sdk
```

Now that all of your dependencies are in place for this plugin let's start writing it.

There are three function hooks that you need to implement.

1. register - Place your initialization code in this function
2. hook_queue - This is where you need to place the code that actually does the work
3. shutdown - Place your de-initialization code in here

### Required classes

You need to first import the following classes into the plugin.

```javascript
var AWS = require("aws-sdk"),
    zlib = require("zlib"),
    util = require('util'),
    async = require("async"),
    Transform = require('stream').Transform;
```

Now you might realize that we didn't define/install `async` as a dependency. Don't worry, it's already included in the
core Haraka server code, enabling you to use in the plugin.

### Initialization

