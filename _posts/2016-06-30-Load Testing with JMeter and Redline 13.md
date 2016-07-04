---
layout: post
title: Scalable and Cheap Load Testing with JMeter, AWS, and Redline13
---

Load testing is an important step in ensuring that your application can perform well under pressure.

Personally I prefer to use separate server monitoring tools/services to gauge server response times and monitor any
errors while a load testing tool will be generating user loads. I've used [New Relic](https://newrelic.com/) with great
success for this. Even their free version is quite capable and will provide decent information about your server
response times and bottlenecks.

There are a lot of services that offer load testing, for a fee. Doing high load high stress and/or long running testing
with these services is expensive.

I have used [Load Impact](https://loadimpact.com/) before and it turned out to be quite expensive if you want to run large
load tests with tens of thousands of concurrent users. And you need to learn their API to write large tests. The
learning curve isn't that steep, but it's a curve non the less.

So the second time around I decided to look around for better alternatives. I've seen JMeter used in some of my
previous engagements, so I decided to give it a try.

I liked it very much, it has a GUI interface with a lot of elements to write your tests. It even has a built in
distributed mode that will let you run distributed testing in master-slave mode. But the master node will be a
bottleneck when running a large number of slaves and is not well suited for large scale distributed scale.

Then I came across a service called [Redline13](https://www.redline13.com), they offer a way to run JMeter tests on AWS.
Provided, of course, that you have one! It will automatically create the amount of on demand AWS EC2 instances and run
the JMeter tests on them. Once the tests are done, the on demand instances are terminated.

The cost is only what Amazon will be charging you for the On-Demand instance usage, which is quite cheap compared to the
other load testing options I've come across. Reline13 also offers a bunch of other features and options for testing, but
for now I'm only interested in the JMeter related functionality.

Lets get into a simple example to see how JMeter and AWS play together.

####JMeter Test

First we need to create a JMeter test. Let's stick to the basics for the purpose of this post.

1. Create a new project (Demo Test) and add a new Thread Group (Demo Test Group) by right clicking on the project,
selecting add and then Thread Group.

![Creating Thread Group]({{ site.baseurl }}/jmeter/JM1.png)

2. Let's simulate 300 users over a 5 second period. Each user will run the requests 10 times before stopping for good.
There's an option to run a user indefinitely if that's what we want, but let's keep things simple for now. Enter 300
for the number of users (threads), 5 (seconds) for the ramp up period and 10 for the loop count. In real life scenarios
you will not be doing 10 requests rapidly without sleeping in between, but let's keep things this way for this example.

![User Detail Configuration]({{ site.baseurl}}/jmeter/JM2.png)

3. Create a simple Get request for a user. Right click on the Thread Group and select Sampler, then HTTP Request.

![Create GET Request]({{ site.baseurl jmeter/JM3.png)

4. For the example we will use http://jmeter.apache.org/. It's a pretty simple page, and I'm sure Apache won't mind
the load since it's the one used in their tutorials. Enter jmeter.apache.org in Server Name, you can also give an IP
address. Select the client implementation, in this case HttpClient4 and finally enter / as the path.

![GET Request Configuration]({{ site.baseurl }}/jmeter/JM4.png)

5. Save the test plan by clicking the save button on top. JMeter will save the test plan with a .jmx extension.

We will generally require a listener added, listeners are how JMeter will let us collect results/metrics from
our results, but they will slow down the test processing time and too much is a hindrance in distributed testing. So we
will not be including any listeners in this particular example.

####Redline13.com

You will need to register yourself first, and then link your account to AWS. The instructions can be found
[here](https://www.redline13.com/Aws/IAMSetup)

1. Once the setup is done you just need to start a new test by navigating to Home and clicking the Start New Test button.

![Creating Thread Group]({{ site.baseurl }}/jmeter/R131.png)

2. Select the JMeter test option and upload the saved .jmx file.

![Creating Thread Group]({{ site.baseurl }}/jmeter/R132.png)

3. Select how many test instances you want. The demo test we created will simulate 300 users, so if we input 3 as the
number of servers we will get 900 users (300 x 3).

4. Run your tests and wait till it's completed.

There you go, the first distributed test has been run, and it's very very cheap, about 3 cents USD.

You can run the test costs further down by using spot instances from AWS if you so wish to.

####Results

There are some metrics provided to us by Redline13.com, we get metrics like requests per second, average response time,
bandwidth usage per request and a bunch of more JMeter and agent performance related metrics.

However I've found that using tools, that will clock the performance from the server, to be a more accurate way to gauge
server response times.

A few things to keep in mind is the load per each AWS instance. This depends heavily on the test written and how fast
the server responds. A faster response time from the server means the tests will need to be run more quickly, consuming
more CPU, limiting the concurrent number of users a instance can support.

For a reali life complex test plan a m3.large instance will probably support about 200-300 users, while more simpler tests
could scale to 500-600 concurrent users. You can also switch to more CPU intensive tests into better suited AWS instance
to gain some performance increase.

You should also try to stay away from expensive operarions like using a lot of listeners or writing results into files.
It will let the tests scale much better.

####Conclusion

Going with JMeter and AWS can save a lot of money when generating load to test your servers, and Redline13.com provides
a convenient way to use AWS for this purpose.

I'm definitely going to be using this option as opposed to the more expensive solutions from now on.


