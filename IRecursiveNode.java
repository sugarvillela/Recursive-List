package recursivelist;

/** Companion to IRecursiveList: provides node functionality for linked list.
 *  Also extends IRecursiveList, so each node has a delegate list to add
 *  tree node functionality as well
 *  Implemented by RecursiveNode */
public interface IRecursiveNode extends IRecursiveList {

    void setNext(IRecursiveNode next);

    IRecursiveNode getNext();

    void setPrev(IRecursiveNode prev);

    IRecursiveNode getPrev();

    void setChildList(IRecursiveList childList);

    IRecursiveList getChildList();

    boolean isRecursive();

    void makeRecursive();

    IRecursiveNode copy();

    /** Override in impl to get and cast to a sub-type node  */
    IRecursiveNode newNode();

    /** Call when removing from list if it simplifies things */
    void unlink();

    void disp();

}
