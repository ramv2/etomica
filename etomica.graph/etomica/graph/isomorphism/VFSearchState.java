package etomica.graph.isomorphism;

import etomica.graph.model.Graph;

public class VFSearchState extends AbstractSearchState {

  private final static byte ST_CORE = 0x01;
  private final static byte ST_TERM_IN = 0x02;
  private final static byte ST_TERM_OUT = 0x04;
  private byte t1in_len;
  private byte t1out_len;
  private byte t2in_len;
  private byte t2out_len;
  private byte[] node_flags_1;
  private byte[] node_flags_2;

  public VFSearchState(Graph g1, Graph g2) {

    super(g1, g2);
    n1 = (byte) getN1().size();
    n2 = (byte) getN2().size();
    core_len = 0;
    t1in_len = t1out_len = 0;
    t2in_len = t2out_len = 0;
    core_1 = new byte[n1];
    core_2 = new byte[n2];
    node_flags_1 = new byte[n1];
    node_flags_2 = new byte[n2];
    for (byte i = 0; i < n1; i++) {
      node_flags_1[i] = 0;
      core_1[i] = NULL_NODE;
    }
    for (byte i = 0; i < n2; i++) {
      node_flags_2[i] = 0;
      core_2[i] = NULL_NODE;
    }
  }

  protected VFSearchState(VFSearchState state) {

    super(state.getG1(), state.getG2());
    n1 = state.n1;
    n2 = state.n2;
    core_len = state.core_len;
    t1in_len = state.t1in_len;
    t1out_len = state.t1out_len;
    t2in_len = state.t2in_len;
    t2out_len = state.t2out_len;
    core_1 = new byte[n1];
    core_2 = new byte[n2];
    node_flags_1 = new byte[n1];
    node_flags_2 = new byte[n2];
    for (byte i = 0; i < n1; i++) {
      node_flags_1[i] = state.node_flags_1[i];
      core_1[i] = state.core_1[i];
    }
    for (byte i = 0; i < n2; i++) {
      node_flags_2[i] = state.node_flags_2[i];
      core_2[i] = state.core_2[i];
    }
  }

  public void addPair(NodePair pair) {

    assert (pair != null);
    byte node1 = pair.getN1();
    byte node2 = pair.getN2();
    // guarantee the preconditions for adding a new pair
    assert (node1 < n1);
    assert (node2 < n2);
    assert (core_len < n1);
    assert (core_len < n2);
    // decrease the correct counter for node1
    byte flags1 = node_flags_1[node1];
    if ((flags1 & ST_TERM_IN) != 0) {
      t1in_len--;
    }
    if ((flags1 & ST_TERM_OUT) != 0) {
      t1out_len--;
    }
    // decrease the correct counter for node2
    byte flags2 = node_flags_2[node2];
    if ((flags2 & ST_TERM_IN) != 0) {
      t2in_len--;
    }
    if ((flags2 & ST_TERM_OUT) != 0) {
      t2out_len--;
    }
    // update each node's flag
    node_flags_1[node1] = ST_CORE;
    node_flags_2[node2] = ST_CORE;
    // update the matched core set
    core_1[node1] = node2;
    core_2[node2] = node1;
    // increase the length of the matched core set
    core_len++;
    // update the flags of the nodes that have edges to node1
    for (byte i = 0; i < getG1().getOutDegree(node1); i++) {
      byte other = getG1().getOutNode(node1, i);
      if ((node_flags_1[other] & (ST_CORE | ST_TERM_IN)) == 0) {
        node_flags_1[other] |= ST_TERM_IN;
        t1in_len++;
      }
      if ((node_flags_1[other] & (ST_CORE | ST_TERM_OUT)) == 0) {
        node_flags_1[other] |= ST_TERM_OUT;
        t1out_len++;
      }
    }
    // update the flags of the nodes that have edges from node1
    for (byte i = 0; i < getG1().getOutDegree(node1); i++) {
      byte other = getG1().getOutNode(node1, i);
      if ((node_flags_1[other] & (ST_CORE | ST_TERM_IN)) == 0) {
        node_flags_1[other] |= ST_TERM_IN;
        t1in_len++;
      }
      if ((node_flags_1[other] & (ST_CORE | ST_TERM_OUT)) == 0) {
        node_flags_1[other] |= ST_TERM_OUT;
        t1out_len++;
      }
    }
    // update the flags of the nodes that have edges to node2
    for (byte i = 0; i < getG2().getOutDegree(node2); i++) {
      byte other = getG2().getOutNode(node2, i);
      if ((node_flags_2[other] & (ST_CORE | ST_TERM_IN)) == 0) {
        node_flags_2[other] |= ST_TERM_IN;
        t2in_len++;
      }
      if ((node_flags_2[other] & (ST_CORE | ST_TERM_OUT)) == 0) {
        node_flags_2[other] |= ST_TERM_OUT;
        t2out_len++;
      }
    }
    // update the flags of the nodes that have edges from node2
    for (byte i = 0; i < getG2().getOutDegree(node2); i++) {
      byte other = getG2().getOutNode(node2, i);
      if ((node_flags_2[other] & (ST_CORE | ST_TERM_IN)) == 0) {
        node_flags_2[other] |= ST_TERM_IN;
        t2in_len++;
      }
      if ((node_flags_2[other] & (ST_CORE | ST_TERM_OUT)) == 0) {
        node_flags_2[other] |= ST_TERM_OUT;
        t2out_len++;
      }
    }
  }

  public void backTrack() {

    // VF does not backtrack
  }

  public SearchState copy() {

    return new VFSearchState(this);
  }

  public void copy(SearchState fromState) {

    assert (fromState != null);
    assert (fromState instanceof VF2SearchState);
    VFSearchState state = (VFSearchState) fromState;
    setG1(fromState.getG1());
    setG2(fromState.getG2());
    n1 = state.n1;
    n2 = state.n2;
    core_len = state.core_len;
    t1in_len = state.t1in_len;
    t1out_len = state.t1out_len;
    t2in_len = state.t2in_len;
    t2out_len = state.t2out_len;
    core_1 = new byte[n1];
    core_2 = new byte[n2];
    node_flags_1 = new byte[n1];
    node_flags_2 = new byte[n2];
    for (byte i = 0; i < n1; i++) {
      node_flags_1[i] = state.node_flags_1[i];
      core_1[i] = state.core_1[i];
    }
    for (byte i = 0; i < n2; i++) {
      node_flags_2[i] = state.node_flags_2[i];
      core_2[i] = state.core_2[i];
    }
  }

  public boolean isDead() {

    return (n1 != n2) || (t1out_len != t2out_len) || (t1in_len != t2in_len);
  }

  public boolean isFeasiblePair(NodePair pair) {

    assert (pair != null);
    byte node1 = pair.getN1();
    byte node2 = pair.getN2();
    // guarantee the preconditions for testing a pair
    assert (node1 < n1);
    assert (node2 < n2);
    assert ((node_flags_1[node1] & ST_CORE) == 0);
    assert ((node_flags_2[node2] & ST_CORE) == 0);
    // check if the node attributes of both nodes are compatible
    if (!getN1(node1).isCompatible(getN2(node2))) {
      return false;
    }
    byte i, other1, other2, flags;
    byte termout1 = 0, termout2 = 0, termin1 = 0, termin2 = 0, new1 = 0, new2 = 0;
    // Check the 'out' edges of node1
    for (i = 0; i < getG1().getOutDegree(node1); i++) {
      other1 = getG1().getOutNode(node1, i);
      if (((flags = node_flags_1[other1]) & ST_CORE) != 0) {
        other2 = core_1[other1];
        if (!getG2().hasEdge(node2, other2) || !getE1(node1, other1).isCompatible(getE2(node2, other2))) {
          return false;
        }
      }
      else {
        if ((flags & ST_TERM_IN) != 0) {
          termin1++;
        }
        if ((flags & ST_TERM_OUT) != 0) {
          termout1++;
        }
        if (flags == 0) {
          new1++;
        }
      }
    }
    // Check the 'in' edges of node1
    for (i = 0; i < getG1().getOutDegree(node1); i++) {
      other1 = getG1().getOutNode(node1, i);
      if (((flags = node_flags_1[other1]) & ST_CORE) != 0) {
        other2 = core_1[other1];
        if (!getG2().hasEdge(other2, node2) || !getE1(node1, other1).isCompatible(getE2(other2, node2))) {
          return false;
        }
      }
      else {
        if ((flags & ST_TERM_IN) != 0) {
          termin1++;
        }
        if ((flags & ST_TERM_OUT) != 0) {
          termout1++;
        }
        if (flags == 0) {
          new1++;
        }
      }
    }
    // Check the 'out' edges of node2
    for (i = 0; i < getG2().getOutDegree(node2); i++) {
      other2 = getG2().getOutNode(node2, i);
      if (((flags = node_flags_2[other2]) & ST_CORE) != 0) {
        other1 = core_2[other2];
        if (!getG1().hasEdge(node1, other1)) {
          return false;
        }
      }
      else {
        if ((flags & ST_TERM_IN) != 0) {
          termin2++;
        }
        if ((flags & ST_TERM_OUT) != 0) {
          termout2++;
        }
        if (flags == 0) {
          new2++;
        }
      }
    }
    // Check the 'in' edges of node2
    for (i = 0; i < getG2().getOutDegree(node2); i++) {
      other2 = getG2().getOutNode(node2, i);
      if (((flags = node_flags_2[other2]) & ST_CORE) != 0) {
        other1 = core_2[other2];
        if (!getG1().hasEdge(other1, node1)) {
          return false;
        }
      }
      else {
        if ((flags & ST_TERM_IN) != 0) {
          termin2++;
        }
        if ((flags & ST_TERM_OUT) != 0) {
          termout2++;
        }
        if (flags == 0) {
          new2++;
        }
      }
    }
    return (termin1 == termin2) && (termout1 == termout2) && (new1 == new2);
  }

  public boolean isGoal() {

    return (core_len == n1) && (core_len == n2);
  }

  public NodePair nextPair(NodePair prev) {

    assert (prev != null);
    byte prev_n1 = prev.getN1();
    byte prev_n2 = prev.getN2();
    byte cond1 = 0, cond2 = 0;
    if (t1out_len > 0 && t2out_len > 0)
      cond1 = cond2 = ST_TERM_OUT;
    else if (t1in_len > 0 && t2in_len > 0)
      cond1 = cond2 = ST_TERM_IN;
    else
      cond1 = ~0;
    if (prev_n1 == NULL_NODE)
      prev_n1 = 0;
    if (prev_n2 == NULL_NODE)
      prev_n2 = 0;
    else
      prev_n2++;
    while (prev_n1 < n1 && (node_flags_1[prev_n1] & cond1) != cond2) {
      prev_n1++;
      prev_n2 = 0;
    }
    while (prev_n2 < n2 && (node_flags_2[prev_n2] & cond1) != cond2) {
      prev_n2++;
    }
    if (prev_n1 < n1 && prev_n2 < n2) {
      return new NodePair(prev_n1, prev_n2);
    }
    return null;
  }
}