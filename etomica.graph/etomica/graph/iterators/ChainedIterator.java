package etomica.graph.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import etomica.graph.model.Graph;
import etomica.graph.model.GraphIterator;

public class ChainedIterator implements GraphIterator {

  private int current = -1;
  private List<Iterator<Graph>> iterators = new ArrayList<Iterator<Graph>>();

  public boolean chainIterator(Iterator<Graph> iterator) {

    if (current > -1) {
      return false;
    }
    iterators.add(iterator);
    return true;
  }

  public void start() {

    current = 0;
  }

  public boolean hasNext() {

    return current >= 0 && current < iterators.size() && iterators.get(current).hasNext();
  }

  public Graph next() {

    if (hasNext()) {
      Graph g = iterators.get(current).next();
      if (!iterators.get(current).hasNext()) {
        current++;
      }
      return g;
    }
    return null;
  }

  public void remove() {

    // no-op
  }
}