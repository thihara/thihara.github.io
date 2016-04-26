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

#### Setting up typescript support

First we need to install typescript. With `npm` this is quite easy, we just need to type the following command.
```
    npm install -g typescript
```

Now let's create a directory named `angular2_starter`, this is going to be where all the actions going to be.

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
 * The `module` option, which let's us decide which way we want our code to be packaged. In this case
   [commonjs](http://wiki.commonjs.org/wiki/CommonJS)

We need to add two additional options to the `tsconfig.json` file now.

1. `outDir` This tells the compiler to output the compiled files to a specific directory.
2.  `rootDir` This tells the compiler the rood directory of our app, it will then only look at files form this directory.

```json
{
    "compilerOptions": {
        "target": "es5",
        "sourceMap": true,
        "experimentalDecorators": true,
        "emitDecoratorMetadata": true,
        "module": "commonjs",
        "noImplicitAny": false,
        "outDir": "build",
        "rootDir": "app"
    },
    "exclude": [
        "node_modules"
    ]
}
```

Now that we have declared we are using the `app` directory as the root for our typescript files, we need to create it
inside the `angular2_starter` directory.

Now we need to start the typescript compiler and tell it to start watching for `.ts` file changes. If you have an IDE,
it will probably do this automatically. I'm using intelliJ WebStorm and it does this automatically.

Otherwise you just need to type in the following command form the directory your `tsconfig.json` file is located.

```tsc --watch```

You will see a message similar to this `Compilation complete. Watching for file changes.`.

Keep in mind that you shouldn't kill this process, keep it running to continually compile the typescript files.

#### Setting up Angular 2

