package etomica.graph.property;

import java.util.HashSet;
import java.util.Set;

import static etomica.graph.model.Metadata.*;
import static etomica.graph.traversal.Traversal.*;


import etomica.graph.model.Graph;
import etomica.graph.traversal.BiconectedNodalPoint;
import etomica.graph.traversal.TraversalVisitor;


public class HasNodalPoint implements Property {

  public boolean check(Graph graph) {

    if (graph == null) {
      return false;
    }
    Set<Byte> rootNodes = new HashSet<Byte>();
    for (byte nodeID = 0; nodeID < graph.getNodeCount(); nodeID++) {
      if (graph.nodes().get(nodeID).getType() == TYPE_NODE_ROOT) {
        rootNodes.add(nodeID);
      }
    }
    // a graph having no field node has no nodal point
    if (graph.getNodeCount() == rootNodes.size()) {
      return false;
    }
    // a graph having exactly one root node has no nodal point
    if (rootNodes.size() == 1) {
      return false;
    }
    // a graph having a single field node has a nodal point only
    // if the field node is not connected to all root nodes
    if (graph.getNodeCount() - rootNodes.size() == 1) {
      return graph.getOutDegree((byte) (graph.getNodeCount() - 1)) != rootNodes.size();
    }
    // invoke a NP traversal starting and return true IFF the visitor
    // detected that the graph has a nodal point
    NPVisitor v = new NPVisitor(graph, rootNodes);
    new BiconectedNodalPoint().traverseAll(graph, v);
    return v.hasNodalPoint();
  }
}

class NPVisitor implements TraversalVisitor {

  private Graph graph;
  private int rootNodesVisited = 0;
  private boolean notNodal = false;
  private boolean isArticulation = false;
  private Set<Byte> rootNodes = new HashSet<Byte>();

  public NPVisitor(Graph graph, Set<Byte> rootNodes) {

    this.graph = graph;
    this.rootNodes = rootNodes;
  }

  public boolean visit(byte nodeID, byte status) {

    if (status == STATUS_VISITED_BICOMPONENT) {
      notNodal = notNodal || (rootNodesVisited == rootNodes.size());
      rootNodesVisited = 0;
    }
    // the next node is an articulation point and should not be processed
    else if (status == STATUS_ARTICULATION_POINT) {
      isArticulation = true;
    }
    // visiting a node in the current biconnected component
    else if (status == STATUS_VISITED_NODE) {
      // if it is an articulation point, ignore it
      if (isArticulation) {
        isArticulation = !isArticulation;
      }
      else if (graph.nodes().get(nodeID).getType() == TYPE_NODE_ROOT) {
        rootNodesVisited++;
      }
    }
    return true;
  }

  public boolean hasNodalPoint() {

    return (!notNodal);
  }
}
