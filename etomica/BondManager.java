package etomica.association;

    
public class AssociationManager /*listener interface*/ {
    
    private AssociationDefinition associationDefinition;
    private final int index = Atom.requestAtomLinkIndex();
    private final AtomIterator atomIterator;
    private final AtomIteratorList bondedAtomIterator = new AtomIteratorList();
    private int bondedAtomCount;
    
    public AssociationManager(AtomIterator iterator, AssociationDefinition definition) {
        associationDefinition = definition;
        atomIterator = iterator;
    }
    
    public void initialize() {
        AtomPairIterator pairIterator = new AtomPairIterator(
            Simulation.instance.space, atomIterator, new AtomIteratorList(atomIterator));
        pairIterator.reset();
        while(pairIterator.hasNext()) {
            AtomPair pair = pairIterator.next();
            if(associationDefinition.isBonded(pair.atom1(), pair.atom2())) {
                pair.atom1().atomLink[index] = AtomLink.makeLinker(pair.atom2(),pair.atom1().atomLink[index], null);
                pair.atom2().atomLink[index] = AtomLink.makeLinker(pair.atom1(),pair.atom2().atomLink[index], null);
            }//end if
        }//end while
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom atom = atomIterator.next();
            if(atom.atomLink[index] != null) associatedAtomIterator.add(atom);
        }
    }
    
    public Atom randomAssociatedAtom() {associatedAtomIterator.randomAtom();}
    
    public int associatedAtomCount() {return associatedAtomIterator.size();}
    
    public void update(Atom atom) {
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom atomB = atomIterator.next();
            if(atomB == atom) continue;
            boolean wasBonded = false;
            //see if B is on the list of bonded atoms for A
            for(AtomLinker link=atom.atomLink[index]; link!=null; link=link.next) {
                if(link.atom == atomB) {
                    wasAssociated = true;
                    break;
                }
            }
            boolean isAssociated = associationDefintion.isAssociated(atom, atomB);
            
            if(isAssociated != wasAssociated) {//something changed -- one is true and the other is false
                if(wasAssociated) { //but not bonded now
                    for(AtomLinker link=atom.atomLink[index]; link!=null; link=link.next) {
                        if(link.atom == atomB) {
                            if(link == atom.atomLink[index]) atom.atomLink[index] = link.next;
                            link.delete();
                            break;
                        }
                    }
                    for(AtomLinker link=atomB.atomLink[index]; link!=null; link=link.next) {
                        if(link.atom == atom) {
                            if(link == atomB.atomLink[index]) atomB.atomLink[index] = link.next;
                            link.delete();
                            break;
                        }
                    }
                    if(atom.atomLink[index] == null) associatededAtomIterator.remove(atom);
                    if(atomB.atomLink[index] == null) associatededAtomIterator.remove(atomB);
                }
                else { //is bonded now but wasn't before
                    if(atom.atomLink[index] == null) associatededAtomIterator.add(atom);
                    if(atomB.atomLink[index] == null) associatededAtomIterator.add(atomB);
                    atom.atomLink[index] = AtomLink.makeLinker(atomB,atom.atomLink[index], null);
                    atomB.atomLink[index] = AtomLink.makeLinker(atom,atomB.atomLink[index], null);
                }
            }//end if(isBonded != wasBonded)
        }//end while
    }//end update
    
    public int associationCount(Atom atom) {
       int count = 0;
       for(AtomLinker link=atom.atomLink[index]; link!=null; link=link.next) count++;
       return count;
    }
}
    