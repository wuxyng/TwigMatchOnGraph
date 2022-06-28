cd /home/work/TwigOnGraph

export CLASSPATH=./bin:/home/xiaoying/work/experiments/jar/RoaringBitmap-0.8.21.jar:/home/xiaoying/work/experiments/jar/guava-19.0.jar:/home/xiaoying/work/experiments/jar/algs4.jar


#BJ without node filtering
java -Xmx16384m main.BJMain ep_lb20.dag inst_lb20_tree_c.qry

#BJ with node filtering
java -Xmx16384m main.BJFltMain ep_lb20.dag inst_lb20_tree_c.qry

#BUP 
java -Xmx16384m main.BUPMain ep_lb20.dag inst_lb20_tree_c.qry

#SIM without node filtering
java -Xmx16384m main.SimMain ep_lb20.dag inst_lb20_tree_c.qry

#SIM with node filtering
java -Xmx16384m main.SimFltMain ep_lb20.dag inst_lb20_tree_c.qry