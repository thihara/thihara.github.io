---
layout: post
title: Typescript primer for getting started with Angular
---

While the new Javascript specification, ES6 offers a lot of improvements, one thing it lacks is a strong static typing
system.

Why would Javascript need strong types? Well it doesn't as it's being used right now, but when dependency
injection comes into play having types will make it easier for the framework to figure things out. It also makes it a
lot easier to use external libraries when their interfaces are well defined.

Also in my opinion having types will make large Javascript projects more maintainable.

Typescript is compiled into Javascript by the typescript compiler.

Let us see quickly, what new functionality does typescript offers that will be useful in Angular 2 apps.

#### Types

So the most important thing using typescript gives us is strong and statically typed object types. The typescript
compiler will throw error in our face when the types are mixed up by mistake.

The syntax for defining types for variables is pretty straightforward.

```typescript
    let variable : type;
```

```typescript
    let address : string = "408, california Ave.";
    let numberOfOccupants : number = 4;

    let familyCar : Car = new Car();
```

With typescript you get generics as well. Generics if you aren't already familiar with it is how we will be able to
enforce types in Collections.

```typescript
    let addresses : Set<string> = new Set();
    addresses.add("405, California Ave.");
    addresses.add(10); //Error, can't place a number into a Set accepting only Strings
```

If you wish to define a dynamically typed variable, that is possible as well, using the `any` type.

```typescript
    let numberOfPeople : any = 4;
    numberOfPeople = "Foo Bar"; //Allowed, because the type of the numberOfPeople variable is any.
```

#### Function parameters and return types

With typescript your function parameters can have types as well. Functions can also define a clear return type.

```typescript
    function checkEngine(car: Car) : boolean {
        //This function returns a boolean type result and accept a parameter named car, which is of type Car.
    }
```

We can define an optional parameter by using the `?` symbol.

```typescript
    //The function can be invoked without passing the specialInstructions variable becasue it's optional.
    function tuneUpEngine(car : Car, specialInstructions?: string) : void{

    }
```

### Interfaces

Due to the dynamic type system in Javascript a function will work regardless of the parameter given, so long as the
parameter has the properties accessed by the function readily available.

Typescript allows this flexibility, but allows us to define the interface of the parameter the function will accept.

```typescript
    // We can now pass any object to this method, so long as it has a, variable of
    // type boolean named engineTuned
    function tuneUpEngine(car : {engineTuned: boolean;}) : void{
        car.engineTuned = true;
    }
```

Instead of dynamically creating the interface, we want the parameter to adhere to, we can name it, using the interface`
keyword.

```typescript
    interface Tunable{
        engineTuned: boolean;

        //This property of the interface is optional, the compiler will not complain if this property was missing
        //from the parameter object.
        paySpecialAttention?: boolean;
    }

    //We can use the interface like this.
    function tuneUpEngine(car : Tunable) : void{
            car.engineTuned = true;
    }

    //Not OK, mandatory field engineTuned is missing.
    tuneUpEngine({paySpecialAttention:false});

    //OK
    tuneUpEngine({engineTuned:false});

    //OK
    tuneUpEngine({engineTuned:false, paySpecialAttention:false});
```

So the next typical question, from someone who has already worked with interfaces before (Java delopers like me for
example), is going to be, can interfaces have methods? Can they be implemented by Classes like in many other languages?

The answer to both questions is going to be yes!

You can define methods inside interfaces, and your classes can implement these interfaces by using the `implements`
keyword.

```typescript
    interface Tunable{
            engineTuned: boolean;
            paySpecialAttention?: boolean;

            tuneEngine(tuneParameters: string) : boolean;
    }

    //The car class implements the Tunable interface. Consequently objects of the Car class can be passed into
    //anywhere a Tunbale is expected.
    class Car implements Tunable{

        engineTuned = false;
        paySpecialAttention = false;

        tuneEngine(tuneParameters: string) : boolean {
            console.log("Engine tuned");
            return true;
        }
    }
```

You can also extend existing interfaces by redefining them and adding the new attributes and / or methods.

```typescript
    //Now Number class has a new method named toBaseWhatever.
    interface Number{
        toBaseWhatever(base: number):string;
    }
```

Keep in mind that interfaces are purely for compile time checking, no changes will be made to the compiled Javascript files
due to the use of interfaces.

Now this is all good and well when we are writing our applications, but can't we get type checking for libraries we
might end up using?

We can, this is achieved by defining the interfaces of those libraries separately. Generally they carry the `.d.ts`
format. There are a bunch of these definitions created by the typescript community member out there.

For example angular type definitions would look like `angular.d.ts`.

You can use it by using a special comment that the typescript compiler recognises.

```typescript
    // <reference path="angular.d.ts" />
```


#### Decorators

Typescript also offers something called decorators, which is a lot similar to Java annotations.

The decorators are a way to provide metadata. You can annotate classes, methods, parameters and object properties with
decorators.

Type script decorators are powerful, they can modify the input and output parameters in the context they are applied to.
They can also provide metadata for frameworks, which is what we'll look at since that's the purpose of typescript
decorators for Angular2.

Decorators are designated by the `@` symbol, and can contain a number of configuration and/or metadata required for
their operation.

```typescript
    //This is a sample of how decorators can be used to provide metadata to the Angular2 framework.
    @Component({
    selector: 'car',
    templateUrl: 'car/car.html'
    })
    export class Car{

    }
```

They can also be used to automate some cross cutting concerns like logging because of their powerful nature.

```typescript

    let log = function () {

        //target : The prototype of the class
        //propertyKey : The name of the method
        //descriptor : Target descriptor, more accurately an instance of TypedPropertyDescriptor describing if the method
        //             is writable, configurable, enumerable etc.
        return (target: any, propertyKey: string, descriptor: any) => {

            //Here we are simply logging that the method was called. But you can do things like manipulating the
            //parameters or return value as well.
            console.log(`Called ${propertyKey}`);

            return descriptor;
        };
    };

    //This is how the decorator will be used.
    @log
    function honkCar(car:Car):void{

    }
```

So this is the most important aspects of Typescript where Angular2 development is concerned. You don't have to use it,
but using it will make your angular app a lot cleaner and easier to read, not to mention well organized and
maintainable.