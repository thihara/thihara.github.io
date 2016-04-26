---
layout: post
title: How to setup an Angular 2 project with Typescript support.
---

There are two ways to create an Angular 2 project.

One is to use ES6, the other is to use Typescript. Typescript offers strong types and a cleaner way to configure
Angular 2 components called `decorators`.

Personally I prefer using typescript. It ticks all the right boxes for me, being from a Java backend. And using
`decorators` for configuring the angular metadata is similar to what I've done with Java annotations.

So how do we set up an Angular 2 project with TypeScript support?

First we will need [npm](https://www.npmjs.com/), which can be installed by following the guidelines from the
[docs](https://docs.npmjs.com/getting-started/installing-node).

First we need to install typescript. With `npm` this is quite easy, we just need to type the following command.
```
    npm install -g typescript
```

Now let's create a directory named angular2_starter, this is going to be where all the actions going to be.

Let's switch into the directory and create the `tsconfig.json` file. This file let's us configure the typescript
compiler.

```
tsc --init --target es5 --sourceMap --experimentalDecorators --emitDecoratorMetadata
```

A `tsconfig.json` should be create in the directory now. It should look something like this.

```json
{
    "compilerOptions": {
        "target": "es5",
        "sourceMap": true,
        "experimentalDecorators": true,
        "emitDecoratorMetadata": true,
        "module": "commonjs",
        "noImplicitAny": false
    },
    "exclude": [
        "node_modules"
    ]
}
```

Few important configurations to note are

 * The `target` option which tells the typescript compiler to compile typescript `.ts` files into ES5 version of javascript
 * The `sourceMap` options, which tells the compiler to keep the mapping files between `.ts` sources and the generated
   `.js` files.
 * The experimentalDecorators and emitDecoratorMetadata enables the typescript decorators, we will be using to configure
   Angular2
 * The `module` option, which let's us decide which way we want our code to be packaged. In this case [commonjs](http://wiki.commonjs.org/wiki/CommonJS)


