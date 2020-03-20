[![codecov](https://codecov.io/gh/28Smiles/jasync-sql-extensions/branch/master/graph/badge.svg)](https://codecov.io/gh/28Smiles/jasync-sql-extensions)
[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](https://github.com/jasync-sql/jasync-sql/blob/master/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.28Smiles/jasync-sql-extensions/badge.svg)](https://mvnrepository.com/artifact/com.github.28Smiles/jasync-sql-extensions)

# jasync-sql-extensions
[jasync-sql-extensions](https://github.com/28Smiles/jasync-sql-extensions) is a simple Extension of [jasync-sql](https://github.com/jasync-sql/jasync-sql). The current Features include:
 - Named argument binding
   - List binding
   - Bean binding
   - Json binding
 - Data class mapping
   - Json mapping
   - Custom mapper support
   - WIP: 1 to N, N to N and 1 to 1 support via dsl

 ## Getting Started
[jasync-sql-extensions](https://github.com/28Smiles/jasync-sql-extensions) is based upon extension functions. So create a query using named bindings start like this:
```
    connection.sendPreparedStatement(
        "SELECT :bert.number, :bert.nabla, :carol.id, :carol.name",
        mapOf(
            "bert" to Bert(
                number = 42, 
                nabla = "bar"
            ),
            "carol" to Carol(
                id = 24,
                name = "Homer"
            ) 
        )
    )
```
To map a result set, use the `mapTo<>()` extension.
```
  resultSet.mapTo<Carol>()
```
This will map the corresponding columns into objects.

### Use in your project
Add this to your `build.gradle` file
```
  compile group: "com.github.28Smiles", name: "jasync-sql-extensions", version: "0.3.1"
```

## Speed
To improve the speed of mapping columns into objects, before mapping the bean is analysed and the most efficient code will be syntesised using ASM. This way the `mapTo<>()` implementation, in most cases, outperforms mapping code created by the kotlin compiler, since some null checks have been ommited.
The `MappingBenchmark`, mapping a result set of 500000 rows shows the following run times:
```
      Manual mapping of the select took: 61 ms
      Reflection mapping of the select took: 233 ms
      Cold ASM mapping of the select took: 86 ms
      Hot ASM mapping of the select took: 51 ms
```
Manual mapping refers to a standard kotlin implementation.
Reflection mapping refers to a mapping using kotlin reflection.
Cold ASM mapping refers to code being synthesized and then executed.
Hot ASM mapping refers code being cached and executed.
Using the `SmallMappingBenchmark`, mapping 30 beans, we get a impression of the initial overhead:
```
      Manual mapping of the select took: 1120 us
      Reflection mapping of the select took: 55562 us
      Cold ASM mapping of the select took: 31021 us
      Hot ASM mapping of the select took: 147 us
```

## Stabillity
This libary is tested, but it most certainly contains bugs and has several limitations, use with caution.
