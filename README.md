# Tree Pattern Matching on Graph
## Introduction
We develop a novel framework to address the problem of efficiently finding homomorphic matches of hybrid tree-patterns over large graphs. These are patterns that include *child*-edges (mapped to edges in the data graph) and/or *descendant*-edges (mapped to paths in the data graph), thus allowing for higher expressiveness and flexibility in query formulation. We introduce the concept of *answer graph* to compactly represent the query results and exploit computation sharing. Leveraging the answer graph, our approach is able to avoid the generation of intermediate results not participating in the query answer, and can produce the query answer in time *linear* on the size of the input data graph and the output. We design a bottom-up algorithm (BUP) and a simulation-based algorithm (SIM) for building the answer graph, each having their own strengths in different cases. We also present two algorithms for enumerating the results using the answer graph. Our extensive experimental evaluation on real and synthetic datasets shows that our proposed techniques greatly outperform the state-of-the-art graph matching algorithms (BJ and TJ) as well as the popular graph dbms Neo4j on evaluating different types of tree-pattern queries.

## Contents

    README ...................  This file
    Scripts ..................  Directory containing scripts to run the algorithms
    Datasets/ ................  Example graphs and queries
    JARs/ ....................  Directory containing external JARs
    TwigOnGraph/ .............  Sources for BUP and SIM as well as BJ
    TwigStackDBFL/ ...........  Sources for the representative TJ approach TwigStackD 
                                using the BFL reachability index
    TwigStackDSSPI/ ..........  Sources for TwigStackD using the SSPI reachability index


## Requirements

Java JRE v1.8.0 or later

## Input
Both the input query graph and data graph are vertex-labeled.
Each graph starts with 't N M' where N is the number of vertices and M is the number of edges. A vertex and an edge are formatted
as 'v VertexID LabelId Degree' and 'e VertexId VertexId' respectively. Note that we require that the vertex
id is started from 0 and the range is [0,N - 1] where V is the vertex set. The following
is an input sample. You can also find sample data sets and query sets under the test folder.

Example:

```zsh
t 5 6
v 0 0 2
v 1 1 3
v 2 2 3
v 3 1 2
v 4 2 2
e 0 1
e 0 2
e 1 2
e 1 3
e 2 4
e 3 4
```

## Contributors

    @Xiaoying Wu

