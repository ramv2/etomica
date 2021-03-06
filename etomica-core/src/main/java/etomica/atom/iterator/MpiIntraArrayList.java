/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.atom.iterator;

import etomica.api.IMoleculeList;
import etomica.atom.MoleculeArrayList;
import etomica.atom.MoleculePair;
import etomica.util.Debug;

/**
 * Returns all pairs formed from a single list of molecules.
 * 
 * 
 * @author Tai Boon Tan
 *
 */
public class MpiIntraArrayList implements MoleculesetIterator, java.io.Serializable {

    /**
     * Construct iterator with an empty list. No iterates will be given until a
     * non-empty list is specified via setList.
     */
    public MpiIntraArrayList() {
        this(new MoleculeArrayList());
    }

    /**
     * Constructs iterator to return pairs from the given list. Requires reset()
     * before first use.
     * 
     * @param list
     */
    public MpiIntraArrayList(IMoleculeList list) {
        setList(list);
    }

    /**
     * Sets iterator in condition to begin iteration.
     */
    public void reset() {
        if (list.getMoleculeCount() < 2) {
            outerIndex = 2;
            innerIndex = 2;
            return;
        }
        outerIndex = 0;
        innerIndex = 0;
        molecules.atom0 = list.getMolecule(0);
    }

    /**
     * Sets iterator such that next is null.
     */
    public void unset() {
        outerIndex = list.getMoleculeCount() - 2;
        innerIndex = list.getMoleculeCount() - 1;
    }

    /**
     * Returns the number of iterates, which is list.size*(list.size-1)/2
     */
    public int size() {
        return list.getMoleculeCount() * (list.getMoleculeCount() - 1) / 2;
    }

    /**
     * Returns the next iterate pair. Returns null if hasNext() is false.
     */
    public IMoleculeList next() {
        if (innerIndex > list.getMoleculeCount() - 2) {
            if (outerIndex > list.getMoleculeCount() - 3) {
                return null;
            }
            outerIndex++;
            molecules.atom0 = list.getMolecule(outerIndex);
            innerIndex = outerIndex;
        }
        innerIndex++;
        molecules.atom1 = list.getMolecule(innerIndex);
        if (Debug.ON && molecules.atom0 == molecules.atom1) {
            throw new RuntimeException("oops");
        }
        return molecules;
    }

    /**
     * Returns 2, indicating that this is a pair iterator
     */
    public int nBody() {
        return 2;
    }

    /**
     * Sets the list that will be used to generate the pairs. Must call reset()
     * before beginning iteration.
     * 
     * @param newList
     *            the new molecule list for iteration
     */
    public void setList(IMoleculeList newList) {
        if (newList.getMoleculeCount() > 1 && newList.getMolecule(0) == newList.getMolecule(1)) {
            throw new RuntimeException("oops");
        }
        list = newList;
        unset();
    }

    /**
     * Returns the list used to generate the pairs.
     */
    public IMoleculeList getList() {
        return list;
    }

    private static final long serialVersionUID = 1L;
    private IMoleculeList list;
    private int outerIndex, innerIndex;
    private final MoleculePair molecules = new MoleculePair();
}
