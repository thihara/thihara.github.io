---
layout: post
title: Classes with Javascript
---

How does a fully fledged Javascript class looks like?

This was the first thing that came to my mind when I got serious about learning Javascript.

Javascript doesn't have a separate concept of classes like in Java. What a Java developer might associate with classes
in javascript are functions that can be used to create objects.

The one I could easily relate to was creating a constructor function, this will act like a class ins Javascript allowing
us to create objects from it.

```javascript
function Car(make, model){
    this.make = make;
    this.model = model;

    this.honk = function(){
        alert("Honk from "+ make + " " + "model");
    };
 }
```

We can create new objects using the constructor function we created above like this

```javascript
var car1 = new Car("Toyota", "Camry");
var car2 = new Car("Rolls Royce", "Phantom");

car1.honk();
```

The other main way of creating an object is to add it's methods to the prototype.

```javascript
function Car(make, model){
    this.make = make;
    this.model = model;
 }

 Car.prototype.honk = function(){
    alert("Honk from "+ make + " " + "model");
 };
```

The part where function attributes are declared `make` and `model` is the same, but the function(s) are added later to
the prototype of the `Car`

These are all public methods and attributes of the `Car` object.

How can we make a method or an attribute private in Javascript?

Private methods and variables are created in the constructor. Any variable declared with var is private, and any function
declared with the `function <function_name` is private too.

```javascript
function Car(make, model){
    var make = make;
    var model = model;

    function honk(){
        alert("Honk from "+ make + " " + "model");
    };
 }
```

In the above example `make` and `mode` variables are private, as is the `honk()` method, they cannot be accessed publicly even
though they are attached to the object.

Private attributes and methods are accessible to functions declared with the `this` keyword. And example is provided below.

```javascript
function Car(make, model){
    var make = make;
    var model = model;

    function honk(){
        alert("Honk from "+ make + " " + "model");
    };

    this.honkAndTurn = function(){
        honk();
        alert("Honked "+make+" and turned");
    };
 }
```




