# PetPet
A sandboxed, dynamically typed scripting language that runs embedded in Java. Being created for my upcoming Minecraft mod!

Huge, *huge* thanks to Robert Nystrom, the creator of [Crafting Interpreters](https://craftinginterpreters.com/). Without that resource, this language would never have been made.

An extension for Visual Studio Code is available on the marketplace which provides syntax highlighting. Search for PetPet to find it; the extension code itself is also available [here](https://github.com/Moonlight-Maya/PetPet-Extension).

## Technical Overview

The "Technical Overview" section describes the different types of expressions that PetPet has to offer, along with their grammar, in detail. If you'd like to skip ahead to a later section which shows some examples, go for it. You'll likely pick it up quick!

### Useful terminology
- An **expression** is a piece of code which resolves to some value. 
- A **name** is a sequence of uppercase or lowercase letters, underscores, or digits. The first character cannot be a digit, and the name cannot be a reserved word in PetPet.

PetPet is what is known as an *expression-based* language, which means that everything that happens in your code is based around these expressions. Here are some examples of basic expressions:

### Basic Expressions

- `5`. This expression evaluates to the number 5.
- `"hello"`. This expression evaluates to a string "hello".
- `null`. This expression evaluates to the null value.
- `false`. This expression evaluates to a boolean value of false.
- `name`. This expression looks up the variable with the given name, and evaluates to the value in that variable.
- `this`. The `this` keyword has an interesting meaning which will be covered later.

### Simple Compound Expressions

Basic expressions like the ones above are the basic building blocks of your code. You can also combine expressions to form new ones, like so. In the following examples, the word "expr" will refer to any expression.

- `expr + expr`. Using a binary operator, you can take the results of two expressions and perform an operation, resulting in a new expression. The binary operators are:
  - `+`. Adds two numbers, or if one of the arguments is a string, returns the concatenation of that string and the other argument, which is converted to a string.
  - `-`. Subtract numbers
  - `*`. Multiply numbers
  - `/`. Divide numbers
  - `%`. Modulo numbers
  - `>`, `<`, `>=`, `<=`. Compare numbers 
  - `==`, `!=`. Check the equality or inequality of two values
  - `&&` or `and`. Check if the two values given are both truthy.
  - `||` or `or`. Check if at least one of the two values given is truthy.
  - `=`. Assigns a value to a variable and evaluates to the value that was assigned. More information on this later.
- `!expr`. Unary operators are placed just before an expression, and do something to that expression.
  - `!` or `not`. If the value it's applied to is truthy, returns false, if falsy, returns true.
  - `-`. Negates a number.
- `(expr)`. An expression with parentheses around it works as you'd expect with the order of operations.
- `{expr expr expr expr}`. Using curly braces `{}`, you can put multiple expressions together. The entire curly-brace expression (called a Block Expression) evaluates to whatever the final expression inside does. If there are no expressions inside, it evaluates to `null`.
- `return expr`. This expression is a bit interesting in that what it evaluates to is never important. This is a special expression that jumps out of whichever function call it is inside, and makes the result of the function call be the provided expression.
- `if expr expr else expr`. The **first** expression is checked, to see if it's truthy. If it is, then the result of the If Expression is the result of the **second** expression. If the first expression was *not* truthy, then the If Expression evaluates to the result of the **third** expression. 
  - The first expression is always evaluated. Only one of either the second or third expressions is evaluated.
  - You may decide to leave off the `else expr` at the end, just writing `if expr expr`. If you do, and the first expression is not truthy, then the overall result will be `null`.
- `while expr expr`. The first expression is checked. If it's truthy, then the second expression (known as the body) will be evaluated, and this will repeat - it will again check the first expression, and if it's truthy, it will execute the second expression, and so on. The overall result of the expression is whatever the body evaluated to on the final iteration. If the body was never evaluated, the overall result is null.
- `![expr, expr, expr]`. A list constructor is created when surrounding multiple expressions, separated by commas, with `![` on the left and `]` on the right. This expression evaluates to a list containing each expression.
- `$[name=expr, name=expr, name=expr]`. A table constructor is made using `$[` and `]`, with values inside being key-value pairs.
  - In the above case, the keys in your table will be strings with the values being the given names.
  - If you want to create a table with keys that are not names, you may use the alternative syntax: `$[[expr]=expr, [expr]=expr, [expr]=expr]`.
- `fn(name, name, name) expr`. This expression can have any (reasonable) number of names inside the `()`, including zero names. This expression evaluates to a function which, when called, will result in the expression. More information on functions later.
- `expr(expr, expr, expr)`. "Calls" the first expression with the expressions inside the parentheses as the arguments. The first expression should evaluate to something that can be called (a function, generally), and the number of parameters of that callable should be the same as the number of expressions passed as arguments. It evaluates to whatever the call to the function does.

### Advanced Compound Expressions

These compound expressions are a bit more esoteric, so they're moved to the advanced section. In here we will find expressions related to **classes**.

- `expr[expr]`. Attempts to "get" from the result of the first expression with the result of the second expression.
  - `expr.name` is shorthand for this. It's equivalent to getting from `expr` with the string of `name` as the second expression.
  - `expr:name` is a "strong get" operation. This will only matter when discussing invocation expressions.
- `expr[expr] = expr`. This is a "set" operation. Attempts to set a value in the first expression, with the key being the second expression, to the result of the third expression.
  - The same alternatives apply as for Get Expressions. `expr.name = expr` and `expr:name = expr` are both legal.
- `expr[expr](args)`. Attempts to invoke a **method** on the result of the first expression. A method is a special type of function that is inside a class.
  - It is also possible to do `expr.name()` here, and this is by far the most common occurrence. Performing an invocation in the general way, through `expr[expr]()`, is only useful when you don't know the name of the method ahead of time.
  - Someone may want to perform a *get* then a *call* instead of an invocation. There are two ways to go about this.
    - If the key is a known string, then the ideal way would be to use the strongly-binding get operator `:`. `math.sin(x)` will attempt to invoke the method `sin` on the type `table`, which will fail. However, `math:sin(x)` will first perform a *get* operation, returning a function, and then a *call* operation, calling that function.
    - If the key is unknown or is not a string, use `{expr[expr]}(args)`.
- `fn name(name, name, name) expr`. This construct is a shortcut for the expression `name = fn(name, name, name) expr`, with the added benefit of the function having a name, which will appear in stack traces in errors.
  - The first name can be any sequence of "get" operators. For example, `fn a:b.c(x) x+2` will be the same as `a:b.c = fn(x) x+2`, and the function will be given the name "c".
  - Placing `global` before the word `fn` will make the variable global.

## Examples

TODO