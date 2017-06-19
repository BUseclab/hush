#Running

When running the analysis a directory sources-sinks will be created containing the SourceAndSinks file for the first pass of the analysis (Network to model deserialization).

#Build

```
mvn install
```

#Assemble

```
mvn assembly:assembly
```

#Results

Each run produces a file [apk name]-results.json
This file contains information about data that was found and hidden

All gets are used for sources in model2ui
If a path is found to a sink from both a model constructor and a get, to the same sink we assume it is derived from an injected constructor in a list and this get is part of the model
