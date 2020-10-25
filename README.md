# SELVa

Simulator of Evolution with Landscape Variation


**Author**: Elena Nabieva

**Affiliation**: Skolkovo Institute of Science and Technology, Moscow, Russia; Kharkevich Institute for Information Transmission Problems, Moscow, Russia.

SELVa is a simulator of sequence evolution that allows the fitness landscape to vary according to user-specified rules.  It is geared towards exploring the effects of landscape change on molecular sequence evolution.  SELVa has a variety of options for specifying the rules of landscape change, allowing the user to tailor the simulation to his or her needs and to explore various evolutionary scenarios.

#### How to run SELVa:
SELVa is distributed as a Java jar file.

Simulation settings are given in a config file.

SELVa runs the simulation along the user-provided phylogenetic tree.  The tree should be provided in a Newick-format file, and the config file should contain the path to this file (as the value of the `TREE_FILE` parameter).

To run the simulator, open the command line prompt, go to the directory where the above-mentioned files are and type

`% java –jar Selva.jar config.txt`

Currently, the jar is built using Java 1.8, so you have to have the corresponding JRE on your system.

### Getting started
We provide some usage examples

example_useset: an example of specifying many parameters of the simulation by hand

example_stochastic: an example of stochastic landscape change with randomly sampled landscapes

example_age: an example of the current allele's fitness increasing with time (age)

example_variable_landsccapes: three landscapes, each governed by its own rules (though in this case they largely overlap)

In each case, the .fasta files are example of output.  In the stochastic case, they will be different for every simulation.

#### What SELVa does:
SELVa simulates point mutations (no indels yet) along a user-provided phylogenetic tree (given in a separate file in the Newick format).  These substitutions are governed by a fitness landscape that is specified by a vector giving the fitness of each allele.  The fitness landscape can change discretely according to rules set by the user in the config file.  The config files also specifies everything else about the simulation, including the sequence alphabet, the length of the sequence, the number of processors used, whether to print the intermediate fitness values, etc.  Detailed information about the config file options is given in the Manual.
