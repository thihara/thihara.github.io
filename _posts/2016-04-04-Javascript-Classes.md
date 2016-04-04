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