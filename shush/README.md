# S-Hush
S-Hush is the automated, scalable software tool used in Hush to find Android
applications with Server-based InFormation OvershariNg (SIFON) vulnerabilities.
Please refer to our
[paper](http://cs-people.bu.edu/wfkoch/my-data/pubs/sifon.pdf) for more details.
 
If you use this code please cite the following paper,
```
@inproceedings{koch2017hush,
  title={Semi-automated discovery of server-based information oversharing
vulnerabilities in Android applications},
  author={Koch, William and Chaabane, Abdelberi and Egele, Manuel and Robertson,
William and Kirda, Engin},
  booktitle={Proceedings of the 26th ACM SIGSOFT International Symposium on
Software Testing and Analysis},
  pages={147--157},
  year={2017},
  organization={ACM}
}
```

# Build

```
mvn install
```

# Assemble

```
mvn assembly:assembly
```

# Running
There is a helper script `run.sh` to run the analysis. View this script and the `edu.bu.android.hiddendata.FindHidden` class to see possible command line options. S-Hush uses FlowDroid thus most command line options are inherited from this tool.

The following command is an example of how to run this script,

```
./run.sh /home/you/shush/apks.txt /home/you/shush/output ./SourcesAndSinks_1.txt
```

The first parameter takes in the location of a text file that contains a list of
APKs to analyize. The absolute path of each APK file is on a separate line.

The second parameter is the directory location where the analysis results will
be placed. 

The last parameter is the source and sink definitions file for the first stage
(i.e., input recieved from a network connection to a deserialization method).

# Results
Running the `run.sh` file will create a directory named after the API. Inside
this directory a file [apk name]-results.json will be created containing the information about data that was found and hidden.

A log file is also created.

