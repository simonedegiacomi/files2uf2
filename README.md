# Files to UF2

This tool can be used to manage UF2 files in the "file container" mode.
I particular, it can:

- Create a new UF2 to contain one or more files;
- Read a UF2 file and extract the files it contains;

[Here](https://github.com/Microsoft/uf2) you can find more about the UF2 file and the 'file container' mode.

## Usage
### Create a UF2 file
Execute the tool with `pack` as the first argument, followed by a list of a pair of file names. Each pair must contain the name of the file you want to pack and the name that the file will have in the UF2 file. Specify the name of the resulting UF2 file as the last argument.

#### Example
Let's suppose that you want to create a UF2 file named `file.uf2` inside a folder called `build` and that the UF2 file should contain the files `build/file.elf` and `build/file.rbf`. You also want that the two files in the container are children of a folder called `Projects` instead of `build`.

The command that you need to execute is then:
```bash
java -jar files2uf2.jar pack build/file.elf Projects/file.elf build/file.rbf Projects/file.rbf build/file.uf2
```

### Unpack a UF2 file
Execute the tool with `unpack` as the first argument, followed by the path to the existing UF2 file and, as the last argument, the name of the folder into which unpack the files.

#### Example
Let's suppose you already have a UF2 file which contains two files: `Project/file.elf` and `Project/file.rbf`. You want to extract these files into a folder named `unpacked`.


The command that you need to execute is then:
```bash
java -jar files2uf2.jar unpack build/file.uf2 unpacked
```

## How to build
The tool is written in Java and you can use Maven to create an executable jar using:
```bash
mvn package
```
You'll find the .jar file inside the generated `target` folder.
