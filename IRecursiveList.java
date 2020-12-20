package recursivelist;

import java.util.ArrayList;

/**A linked list with recursion, so it can be a tree if needed
 * List nodes also implement RecursiveList interface, so each node can be a linked list
 *
 * Access top-level members with push, pop, peek: from front, back and middle
 * Access top-level sub-lists with push, pop, peek: from front, back and middle: 1 extra parameter for range
 * Supports negative indexing for all top-level access methods: -1 for last item
 *
 * Access lower levels of tree using pushBelow, popBelow: variable parameters for arbitrary depth
 * Also can use RecursiveNode method to get child list
 *
 * Flat iterator parses top level only
 * Depth-first iterator parses all, skipping the branch nodes: disp() for example
 *
 * Get list as flat array (top level) or breadth-first array: all nodes including branches
 *
 * Notes:
 * 1. Negative indexing: be aware that negative indexing is 1-index because -0 is not a thing
 *    0 gives first item, 1 gives second item etc. But -1 gives the last item in list.
 * 2. Linked list structure uses node state to maintain list structure.  You can easily
 *    break the list when working with nodes, so...
 *      Methods that return a RecursiveList use copies of the nodes.
 *        --Prev and next links are recreated in new list so they don't affect the old list.
 *      Methods that return array keep the original nodes.
 *        --Prev and next links are preserved; changes to new list nodes will affect the old list.
 *      Methods that return a node return the original node
 *        --Prev and next links are preserved; changes to the node will affect the list.
 *      In every case, node payload is preserved and will test equal, even when node is copied.
 * 3. Iterators do not implement java Iterable, so just use a while loop: while(hasNext()) node = next() */

public interface IRecursiveList {
    int size();
    int indexOf(IRecursiveNode target);
    boolean seek(int index);
    boolean seek(IRecursiveNode target);

    IRecursiveNode peekFront() ;
    IRecursiveNode peekBack() ;
    IRecursiveNode peekIn( int index );

    IRecursiveList peekFront(int hi);
    IRecursiveList peekBack(int lo);
    IRecursiveList peekIn(int lo, int hi);

    IRecursiveList peekFront(IRecursiveNode last);
    IRecursiveList peekBack(IRecursiveNode first);
    IRecursiveList peekIn(IRecursiveNode first, IRecursiveNode last);

    void pushFront(IRecursiveNode newHead);
    void pushBack (IRecursiveNode newTail);
    void pushIn(int index, IRecursiveNode newNode);

    void pushFront(IRecursiveList newFront);
    void pushBack (IRecursiveList newBack);
    void pushIn(int index, IRecursiveList newIn);

    IRecursiveNode popFront();
    IRecursiveNode popBack();
    IRecursiveNode popIn(int index);

    IRecursiveList popFront(int hi);
    IRecursiveList popBack(int lo);
    IRecursiveList popIn(int lo, int hi);

    void removeFront(int hi);
    void removeBack(int lo);
    void removeIn(int lo, int hi);

    /*=====Recursive access===========================================================================================*/

    void setLevel(int level);
    int getLevel();

    void pushBelow(IRecursiveList childList, int... params);
    IRecursiveList popBelow(int... params);


    /** New empty list, override subclasses to make the right type */
    IRecursiveList newList();

    /** Methods that return a RecursiveList use copies of the nodes
     *  Prev and next links are recreated in new list so they don't affect the old list
     *  Node payload is preserved and will test equal */

    IRecursiveList copy();
    IRecursiveList reverse();

    /** Methods that return array keep the original nodes.
     * Prev and next links are preserved; changes to new list nodes will affect the old list.
     * 'flat' structures give the top layer of the tree only */
    ArrayList<IRecursiveNode> toFlatArrayList();
    IRecursiveNode[] toFlatArray();
    IRecursiveNode[][][] toBreadthFirstArray();

    ListItr getFlatIterator();
    ListItr getDepthFirstIterator();

    /** A recursive utility method to reset each node's 'level' field to its position in the
     *  current list/tree. 'Level' corresponds to the node's depth in the tree.
     * @param setLevel Pass 0 to start; each recursion adds 1 until leaf is reached
     * @return the highest level assigned, giving the max depth of the tree */
    int refreshLevels(int setLevel);

    /* Debug */
    void disp();
    void disp(int indent);

    interface ListItr {
        /** Enable Concurrent Modification Exception if list is modified while iterating
         *  Parent list can set on getFlatIterator() etc. Clears on haveNext() == false */
        void setBusy();
        void clearBusy();

        /**Supports negative indexing for start and end; back iteration using the increment parameter
         * Be aware: negative indexing is 1-index because -0 is not a thing
         * 0 gives first item, 1 gives second item etc. But -1 gives the last item in list.
         * @param start zero-index from front or -1-index from back
         * @param end   zero-index from front or -1-index from back
         * @param increment 1 or more to go forward; -1 or less to go backward
         */
        void setRange(int start, int end, int increment);
        void setRange(int start, int end);

        void clearRange();
        void iterateBack();

        void rewind();

        boolean hasNext();
        IRecursiveNode next();
        IRecursiveNode peekNext();

        int key();
        int level();
    }

    interface SimpleListener{
        void onSizeChanged();
    }
}
