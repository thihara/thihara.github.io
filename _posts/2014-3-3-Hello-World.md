---
layout: post
title: Setting up the Jekyll blog!
---

Setting up Jekyll on git hub as a personal blog wasn't that hard. But deciding what to write on it wasn't!

[Github](https://github.com/) provides free hosting for personal blogs. And you get the initial URL of `username.github.io`,
which can be changed if you have a [CNAME record](https://en.wikipedia.org/wiki/CNAME_record) by simpy
creating a file named `CNAME`.

Setting up your site this way will make your life easier, you don't have to deal with any hosting companies
or any of the administrative things that are needed for hosting your own site. And the default `username.github.io` is a very
reasonably personalized URL.

You use git to manage all your changes, so it's easy to deploy (as easy as doing a push to master), you can roll back
to any point you want so long as it has a git commit id.

Github supports [Jekyll](https://jekyllrb.com/) which is a static site generator, you can use
[Markdown](https://daringfireball.net/projects/markdown/) to write your pages and Jekyll will render them as HTML.

While you can write your own site from scratch, you can get started quickly by using an existing Jekyll site template,
while you can find a few on github I chose [Jekyllnow](https://github.com/barryclark/jekyll-now).

First you need to clone the repository and rename it to `username.github.io` , github hosting will work only if you enter
the username correctly.

Then change a few things in the `_config.yml` file to make the site display your details, instead of the defaults and
push your code to the master branch. You can't see anything on `username.github.io` URL untill you push to the repository.
It looks like the deployment is hooked to the push action.

Your blog is up and running now, just create your first blog entry, or stare at the screen thinking about what to write.