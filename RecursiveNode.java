package recursivelist;

import java.util.ArrayList;

public abstract class RecursiveNode implements IRecursiveNode {
    private IRecursiveNode prev, next;
    private IRecursiveList childList;
    protected final Object payload;

    public RecursiveNode(Object payload) {
        this.payload = payload;
    }

    /** Copy constructor: no links to original?
     *    Since prev and next are copied, the node is still technically in the list
     *    But you can edit or null the prev and next fields without breaking the list
     *  Object.equals() would return false, except equals() is overridden in the class
     *    Copied nodes test equal because equals() compares payload field
     * @param sourceNode object to be copied */
    public RecursiveNode(RecursiveNode sourceNode){
        this.childList = (sourceNode.getChildList() == null)? null : sourceNode.getChildList().copy();
        this.payload = sourceNode.payload;
        prev = sourceNode.getPrev();
        next = sourceNode.getNext();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RecursiveNode && this.payload.equals(((RecursiveNode)obj).payload);
    }

    /* =====IRecursiveNode methods for in-node manipulation of prev, next and childList============================== */

    @Override
    public void setNext(IRecursiveNode next) {
        this.next = next;
    }

    @Override
    public IRecursiveNode getNext() {
        return next;
    }

    @Override
    public void setPrev(IRecursiveNode prev) {
        this.prev = prev;
    }

    @Override
    public IRecursiveNode getPrev() {
        return prev;
    }

    @Override
    public void setChildList(IRecursiveList childList){
        this.childList = childList;
    }

    @Override
    public IRecursiveList getChildList(){
        return this.childList;
    }

    @Override
    public boolean isRecursive() {
        return this.childList != null;
    }

    @Override
    public void unlink(){
        next = prev = null;
        childList = null;
    }

    /* =====IRecursiveList methods delegated to childList instance: no null check==================================== */

    @Override
    public void disp(){
        childList.disp();
    }

    @Override
    public void disp(int indent){
        childList.disp(indent);
    }

    @Override
    public int size() {
        return childList.size();
    }

    @Override
    public int indexOf(IRecursiveNode target) {
        return childList.indexOf(target);
    }

    @Override
    public boolean seek(int index) {
        return childList.seek(index);
    }

    @Override
    public boolean seek(IRecursiveNode target) {
        return childList.seek(target);
    }

    @Override
    public IRecursiveNode peekFront() {
        return childList.peekFront();
    }

    @Override
    public IRecursiveNode peekBack() {
        return childList.popBack();
    }

    @Override
    public IRecursiveNode peekIn(int index) {
        return childList.peekIn(index);
    }

    @Override
    public IRecursiveList peekFront(int hi) {
        return childList.peekFront(hi);
    }

    @Override
    public IRecursiveList peekBack(int lo) {
        return childList.peekBack(lo);
    }

    @Override
    public IRecursiveList peekIn(int lo, int hi) {
        return childList.peekIn(lo, hi);
    }

    @Override
    public IRecursiveList peekFront(IRecursiveNode last) {
        return childList.peekFront(last);
    }

    @Override
    public IRecursiveList peekBack(IRecursiveNode first) {
        return childList.peekBack(first);
    }

    @Override
    public IRecursiveList peekIn(IRecursiveNode first, IRecursiveNode last) {
        return childList.peekIn(first, last);
    }

    @Override
    public void pushFront(IRecursiveNode newHead) {
        childList.pushFront(newHead);
    }

    @Override
    public void pushBack(IRecursiveNode newTail) {
        childList.pushBack(newTail);
    }

    @Override
    public void pushIn(int index, IRecursiveNode newNode) {
        childList.pushIn(index, newNode);
    }

    @Override
    public void pushFront(IRecursiveList newFront) {
        childList.pushFront(newFront);
    }

    @Override
    public void pushBack(IRecursiveList newBack) {
        childList.pushBack(newBack);
    }

    @Override
    public void pushIn(int index, IRecursiveList newIn) {
        childList.pushIn(index, newIn);
    }

    @Override
    public IRecursiveNode popFront() {
        return childList.popFront();
    }

    @Override
    public IRecursiveNode popBack() {
        return childList.popBack();
    }

    @Override
    public IRecursiveNode popIn(int index) {
        return childList.popIn(index);
    }

    @Override
    public IRecursiveList popFront(int hi) {
        return childList.popFront(hi);
    }

    @Override
    public IRecursiveList popBack(int lo) {
        return childList.popBack(lo);
    }

    @Override
    public IRecursiveList popIn(int lo, int hi) {
        return childList.popIn(lo, hi);
    }

    @Override
    public void removeFront(int hi) {
        childList.removeFront(hi);
    }

    @Override
    public void removeBack(int lo) {
        childList.removeBack(lo);
    }

    @Override
    public void removeIn(int lo, int hi) {
        childList.removeIn(lo, hi);
    }

    @Override
    public void setLevel(int level) {
        if(childList != null){
            childList.setLevel(level);
        }
    }

    @Override
    public int getLevel() {
        return childList.getLevel();
    }

    @Override
    public void pushBelow(IRecursiveList childList, int... params) {
        this.childList.pushBelow(childList, params);
    }

    @Override
    public IRecursiveList popBelow(int... params) {
        return childList.popBelow(params);
    }

    @Override
    public IRecursiveList newList() {
        return childList.newList();
    }

    @Override
    public IRecursiveList reverse() {
        return childList.reverse();
    }

    @Override
    public ArrayList<IRecursiveNode> toFlatArrayList() {
        return childList.toFlatArrayList();
    }

    @Override
    public IRecursiveNode[] toFlatArray() {
        return childList.toFlatArray();
    }

    @Override
    public IRecursiveNode[][][] toBreadthFirstArray() {
        return childList.toBreadthFirstArray();
    }

    @Override
    public final int refreshLevels(int setLevel){
        return childList.refreshLevels(setLevel);
    }

    @Override
    public ListItr getFlatIterator() {
        return childList.getFlatIterator();
    }

    @Override
    public ListItr getDepthFirstIterator() {
        return childList.getDepthFirstIterator();
    }
}
