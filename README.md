# Tree Pattern Matching on Graph
## Introduction
We develop a novel framework to address the problem of efficiently finding homomorphic matches of hybrid tree-patterns over large data graphs. These are patterns that include *child*-edges (mapped to edges in the data graph) and/or *descendant*-edges (mapped to paths in the data graph), thus allowing for higher expressiveness and flexibility in query formulation. We introduce the concept of *answer graph* to compactly represent the query results and exploit computation sharing. Leveraging the answer graph, our approach is able to avoid the generation of intermediate results not participating in the query answer and can produce the query answer in time *linear* on the size of the input data graph and the output. We design a bottom-up algorithm (BUP) and a graph simulation-based algorithm (SIM) for building the answer graph, each having their own strengths in different cases. We also present two algorithms for enumerating the results using the answer graph. Our extensive experimental evaluation on real and synthetic datasets shows that our proposed techniques greatly outperform the state-of-the-art graph matching algorithms (BJ and TJ) as well as the popular graph DBMS Neo4j on evaluating different types of tree-pattern queries.


## Contents

    README ...................  This file
    twigOnGraph ..............  Script to run the algorithms
    test/  ...................  Example graphs and queries
    JARs/ ....................  External JARs
    TwigOnGraph/ .............  Sources for BUP and SIM as well as EJ
    TwigOnGraph_bas/ .........  Sources for basic version of BUP and SIM
    TwigStackDBFL/ ...........  Sources for the representative PJ approach TwigStackD 
                                using the BFL reachability index
    TwigStackDSSPI/ ..........  Sources for TwigStackD using the SSPI reachability index


## Requirements

Java JRE v1.8.0 or later

## Input
Both the input query graph and data graph are vertex-labeled. A vertex and an edge are formatted
as 'v VertexID LabelID' and 'e VertexID VertexID EdgeType' respectively. Note that we require that the vertex
id ranges over [0,|V| - 1] where V is the vertex set. The 0 and 1 values of EdgeType denote a child and a descendant edge, respectively. The following is an input sample. You can also find sample data sets and query sets in the test folder.

Example:

```
#q 0
v 0 0 
v 1 1 
v 2 2 
v 3 1 
v 4 2 
e 0 1 0
e 0 2 0
e 1 2 1 
e 1 3 1
e 2 4 0
e 3 4 0
```

## Experiment Datasets

The real world datasets and the corresponding query sets used in our paper as well as cypher queries used for the comparisons with neo4j can be downloaded [here](https://drive.google.com/drive/folders/1JTpypF_-LEBOaeRvCEIuwXAE7bjSt6ST?usp=sharing). We do not upload the synthetic datasets since they are large and can be generated by following the instructions in our paper.

## Contributors

    @Xiaoying Wu

