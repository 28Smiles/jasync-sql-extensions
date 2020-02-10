[![codecov](https://codecov.io/gh/28Smiles/jasync-sql-extensions/branch/master/graph/badge.svg)](https://codecov.io/gh/28Smiles/jasync-sql-extensions)
[![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](https://github.com/jasync-sql/jasync-sql/blob/master/LICENSE)

# jasync-sql-extensions
[jasync-sql-extensions](https://github.com/28Smiles/jasync-sql-extensions) is a simple Extension of [jasync-sql](https://github.com/jasync-sql/jasync-sql). The current Features include:
 - Named argument binding
 - Data Class Mapping
 
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
    )).get()
```
To map a result set, use the `mapTo<>()` extension.
```
  resultSet.mapTo<Carol>()
```
This will map the corresponding columns into objects.

## Speed
To improve the speed of mapping columns into objects, before mapping the bean is analysed and the most efficient code will be syntesised using ASM. This way the `mapTo<>()` implementation, in most cases, outperforms mapping code created by the kotlin compiler, since some null checks have been ommited.
The `MappingBenchmark`, mapping a result set of 500000 rows shows the following run times:
```
      Manual mapping of the select took: 61
      Reflection mapping of the select took: 233
      Cold ASM mapping of the select took: 86
      Hot ASM mapping of the select took: 51
```
Manual mapping refers to a standard kotlin implementation.
Reflection mapping refers to a mapping using kotlin reflection.
Cold ASM mapping refers to code being synthesized and then executed.
Hot ASM mapping refers code being cached and executed.
