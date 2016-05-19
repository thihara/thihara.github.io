---
layout: post
title: Displaying data with Angular 2 Templates and Components
---

One of the first things that comes to mind when plaing with a new front-end framework is how can we display data?

Angular 2 provides `templates` for this purpose, with it's own syntax for data displaying, event binding and some other
bells and whistles.

A `template` is essentially `html` with some syntax specific to Angular 2 thrown in.

Remember the good old `Component` in Angular 2 that acts as the base for pretty much every UI interaction? Well that's
where you can define your template. You can do this inline, or keep a separate `html` file.

Remember the `HelloApp` component form the post about setting up Angular 2? That has an inline template defined.

```typescript
import {Component} from "angular2/core";

@Component({
    selector: "hello-app",
    template:`<h1>Hello from Angular 2!</h1>`
})
export class HelloApp {

}
```

We can define an external `html` file by defining it's URL with the `templateUrl` option.

```typescript
import {Component} from "angular2/core";

@Component({
    selector: "hello-app",
    templateUrl: "view/hello.html"
})
export class HelloApp {

}
```

The `hello.html` will of course contain the `html` markup for the template.

```html
<h1>Hello from Angular 2!</h1>
```

#### Interpolation.

We can user double curly braces `{{}}` to bind Component class properties in the `html` template.

```typescript
import {Component} from "angular2/core";

@Component({
    selector: "hello-app",
    template:`<h1>Hello {{name}}</h1>`
})
export class HelloApp {
    name:string = "Obi Wan";
}
```

The rendered html will then look like `<h1>Hello Obi Wan</h1>`.

Now something to keep in mind is that if the component property changed it's value, Angular 2 will detect the change
and display it. Usually this happens as a result of some kind of asynchronous operation.

The `{{}}` are also capable of evaluating simple expressions.

```html
<h1>We are now in {{1 + getNextInteger()}}</h1>
```

This expression will add `1` to the value returned by `getNextInteger()` and display it.


#### Built in directives.

Angular 2 provides `*ngFor` directive for looping over a collection of objects.


```typescript
import {Component} from "angular2/core";

@Component({
    selector: "hello-app",
    template:`  <h1>Car List</h1>
                <p>Cars:</p>
                <ul>
                  <li *ngFor="let car of cars">
                    {{ car.name }}
                  </li>
                </ul>`
})
export class HelloApp {
    cars:any = [{name:"Camry",make:"Toyota"},{name:"Prius",make:"Toyota"}];
}
```

This snippet will output two `li` elements with `Camry` and `Prius` values. Note how we are declaring the for each
loop `for car of cars`.

We can use `*ngIf` to execute conditional statements.

```html
<span *ngIf="cars.length < 2">
    //Whatever we place here will be displayed if the cars has less than 2 elements.
</span>
```

These should get you started on displaying data in an Angular 2 application.
