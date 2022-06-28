cd /home/work/TwigListD

export CLASSPATH=./bin:/home/work/JARs/RoaringBitmap-0.8.21.jar:/home/work/JARs/commons-cli-1.5.0.jar





#BUP for query answer counting
java main.TGMain_bup ep_lb20.dag inst_lb20_tree_c.qry

#BUP for query answer enumeration using dynamic programming method (DP).

java main.TGMain_bup_enum ep_lb20.dag inst_lb20_tree_c.qry


#BUP for query answer counting
java main.TGMain_bup ep_lb20.dag inst_lb20_tree_c.qry

#BUP for query answer enumeration using dynamic programming method (DP).
java main.TGMain_bup_enum ep_lb20.dag inst_lb20_tree_c.qry
