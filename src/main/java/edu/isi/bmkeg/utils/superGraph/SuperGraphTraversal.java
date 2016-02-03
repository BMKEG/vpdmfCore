package edu.isi.bmkeg.utils.superGraph;



/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class SuperGraphTraversal  {
  private Vector roots = new Vector();

  public ArrayList<SuperGraphNode> nodeTraversal = new ArrayList<SuperGraphNode>();
  public ArrayList<SuperGraphEdge> edgeTraversal = new ArrayList<SuperGraphEdge>();
  private SuperGraph graph;

  /**
   *       Implement the following algorithm (from 'mastering algorithms in
   *       Perl, pg 338):
   *
   *        SSSP-DAG ( graph g, node u)
   *
   *        for every vertex u in topological sort of vertices of G
   *        do
   *
   *          for every successor vertex of u called v
   *          do
   *
   *            relax edge from u to v
   *
   *          done
   *
   *        done
   *
   */



  /**
   *       Implement the following algorithm (from 'mastering algorithms in
   *       Perl, pg 307):
   *
   *        bfs ( graph G, Node u)
   *
   *     create a queue with u as the initial node
   *
   *        mark Node u as seen
   *
   *        while there are vertices in the queue
   *
   *        do
   *
   *          dequeue vertex v
   *
   *          mark v as seen
   *
   *          enqueue unseen neighboring vertices of v
   *
   *        done
   *
   */
  public void bfs ()
  {
        this.setRootNodes();
        Iterator it = this.roots.iterator();
        while(it.hasNext()) {
          this.bfs((SuperGraphNode) it.next());
        }

  }

  private void bfs (SuperGraphNode n)
  {
        Vector queue = new Vector();
        queue.add(n);

        try{

          while(queue.size() > 0) {

            SuperGraphNode currentNode = (SuperGraphNode) queue.get(0);
            queue.removeElementAt(0);

            currentNode.setTag(1);
            this.nodeTraversal.add(currentNode);

            Iterator it = n.getOutgoingEdges().values().iterator();
            while(it.hasNext()) {
              SuperGraphEdge edge = (SuperGraphEdge) it.next();
              SuperGraphNode node = (SuperGraphNode) edge.getInEdgeNode();
              if( node.getTag() == 0 ) {
                queue.add(edge);
              }
            }

          }

        } catch (Exception e){

          e.printStackTrace();

        }


  }

  public void buildEdgeTraversal ()
  {

        //
        // Translate the traversal order of nodes to edges.
        try {

          for(int i=1; i<nodeTraversal.size(); i++) {

            Hashtable table = new Hashtable();
            SuperGraphEdge thisEdge = null;
            SuperGraphNode thisNode = (SuperGraphNode) nodeTraversal.get(i);

            Iterator it = thisNode.getIncomingEdges().values().iterator();
            EDGESEARCH: while(it.hasNext()) {
              SuperGraphEdge thatEdge = (SuperGraphEdge) it.next();
              SuperGraphNode thatNode = thatEdge.getOutEdgeNode();
              int pos = nodeTraversal.indexOf(thatNode);

              if( pos < i && pos != -1 ) {
                Integer temp = new Integer(pos);
                table.put( temp, thatEdge);
              }

            }

            Object[] positions = table.keySet().toArray();
            Arrays.sort(positions);
            for(int j=0; j<positions.length; j++) {
              Integer position = (Integer) positions[j];
              this.edgeTraversal.add( (SuperGraphEdge) table.get(position) );
            }

          }

        } catch (Exception e){

          e.printStackTrace();

        }


  }

  public void destroy ()
  {
    this.graph = null;
    this.nodeTraversal = null;
    this.edgeTraversal = null;
  }

  /**
   *  Implement the following algorithm (from 'mastering algorithms in
   *  Perl, pg 302):
   *
   *  dfs ( graph G, Node u)
   *
   *     mark Node u as seen
   *
   *     for every unseen neighboring Node of u called v
   *
   *     do
   *
   *        dfs v
   *
   *     done
   *
   */
  public void dfs ()
  {
        this.setRootNodes();
        Iterator it = this.roots.iterator();
        while(it.hasNext()) {
          this.dfs(null, (SuperGraphNode) it.next());
        }

  }

  private void dfs (SuperGraphEdge e, SuperGraphNode n)
  {
        try{

          n.setTag(1);
          if(e != null) {
            this.nodeTraversal.add(n);
          }

          Iterator it = n.getOutgoingEdges().values().iterator();
          while(it.hasNext()) {
            SuperGraphEdge edge = (SuperGraphEdge) it.next();
            SuperGraphNode node = (SuperGraphNode) edge.getInEdgeNode();

            if(node.getTag() == 0) {
              this.dfs(edge, node);
            }

          }

        } catch (Exception ex){

          ex.printStackTrace();

        }


  }

  public SuperGraph get_graph ()
  {
    return this.graph;
  }

  public Vector get_roots ()
  {
    return this.roots;
  }

  public SuperGraphTraversal (SuperGraph g)
  {
    this.graph = g;
  }

  /**
   *  We set the root Node to be a node from the unseenNodes Vector with the
   *  smallest number of inEdges (as long as the number of outEdges is greater
   *  than zero).
   *
   */
  public void setRootNodes ()
  {
        this.nodeTraversal.removeAll(this.nodeTraversal);

        //
        // Use the nodes with the fewest in-edges as the root Nodes
        // of the traversal.
        int size = 1000;
        SuperGraphNode currNode = null;

        Iterator it = this.graph.getNodes().values().iterator();
        while(it.hasNext()) {
          SuperGraphNode node = (SuperGraphNode) it.next();
          node.setTag(0);
          int currSize = node.getIncomingEdges().size();
          if( currSize < size && node.getOutgoingEdges().size() > 0){
            size = currSize;
          }
        }

        it = this.graph.getNodes().values().iterator();
        while(it.hasNext()) {
          SuperGraphNode node = (SuperGraphNode) it.next();
          if( node.getIncomingEdges().size() == size ) {
            this.roots.add(node);
          }
          // Also add isolated nodes to the Root nodes vector.
          else if( node.getIncomingEdges().size() == 0 ) {
            this.roots.add(node);
          }

        }


  }

  public void set_graph (SuperGraph graph)
  {
    this.graph = graph;
  }

  public void set_roots (Vector roots)
  {
    this.roots = roots;
  }

  /**
   *  This traversal is the same as the bfs except that we don't add a given
   *  node to the queue until all inEdges of that node connect to nodes that
   *  have been included in the list already
   *
   */
  public void traverseDependency ()
  {
        this.setRootNodes();

        Vector queue = new Vector();
        queue.addAll(this.roots);

        //
        // Loop to get the traversal order of nodes.
        try{

          while(queue.size() > 0) {

            SuperGraphNode currentNode = (SuperGraphNode) queue.get(0);
            queue.removeElementAt(0);
            this.nodeTraversal.add( currentNode );
            currentNode.setTag(1);

            Iterator it = currentNode.getOutgoingEdges().values().iterator();
            while(it.hasNext()) {
              SuperGraphEdge edge = (SuperGraphEdge) it.next();
              SuperGraphNode node = (SuperGraphNode) edge.getInEdgeNode();

              if( node.getTag() == 0 ) {

                //
                // Have we visited all the inEdges of our current node
                boolean okToGoFlag = true;
                Iterator inEdgeIt = node.getIncomingEdges().values().iterator();
                while( inEdgeIt.hasNext() ) {
                  SuperGraphEdge inEdge = (SuperGraphEdge) inEdgeIt.next();
                  if( inEdge.getOutEdgeNode().getTag() == 0) {
                    okToGoFlag = false;
                  }
                }

                if(okToGoFlag) {
                  queue.add(node);
                }

              }

            }

          }

        } catch (Exception e){

          e.printStackTrace();

        }



  }


};
