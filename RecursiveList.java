package recursivelist;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/** Linked list with front, back, middle access; forward, back, multi-step iteration.
 *  Works with IRecursiveNode to allow recursion of any list node.
 *  Each node's payload is another IRecursiveList */
public class RecursiveList implements IRecursiveList{
    protected static final int NUM_LISTENERS = 3;   // number of iterator impl + 1, for AccessUtil
    protected final SharedState shared;
    protected final AccessUtil access;
    protected final SimpleListener[] listeners;     // internal listeners for size change
    protected FlatItr flatItr;                      // ListItr impl
    protected DepthFirstItr depthFirstItr;

    public RecursiveList(){
        shared = new SharedState(0);
        access = new AccessUtil(shared);
        listeners = new SimpleListener[NUM_LISTENERS];
        listeners[0] = access;
        shared.setListeners(listeners);
    }

    private static class SharedState{
        private IRecursiveNode head, tail;        // for doubly linked list role
        private int top;                          // top = length-1
        private SimpleListener[] listeners;       // update AccessUtil, ListItr
        private int level;

        private SharedState(int level){
            this.level = level;
            head = tail = null;
            top = -1;
        }
        private void setListeners(SimpleListener[] listeners){
            this.listeners = listeners;
        }

        private void notifyListeners(){
            for(SimpleListener listener : listeners){
                if(listener != null){
                    listener.onSizeChanged();
                }
            }
        }

        protected final void incSize(){
            top++;
            notifyListeners();
        }

        protected final void decSize(){
            top--;
            notifyListeners();
        }

        protected final void incSize(int inc){
            top += inc;
            notifyListeners();
        }

        protected final void decSize(int inc){
            top -= inc;
            notifyListeners();
        }

        @Override
        public String toString() {
            int numListeners = 0;
            for(SimpleListener listener : listeners){
                if(listener != null){
                    numListeners++;
                }
            }
            return "  SharedState{" +
                    "\n    head: " + head +
                    "\n    tail: " + tail +
                    "\n    top: " + top +
                    "\n    top: " + level +
                    "\n    # listeners: " + numListeners +
                    "\n  }";
        }
    }

    private static class AccessUtil implements SimpleListener{
        private static final boolean THROW_ON_ERR = true;  // assertValidIndex()
        private final SharedState shared;                   // Shares head, tail, top with parent list
        private IRecursiveNode curr;                        // Access pointer
        private int row;                                    // Current index of access pointer

        private AccessUtil(SharedState shared) {
            this.shared = shared;
            curr = null;
            row = 0;
        }

        // Allow positive or negative indexing
        private int fixNegIndex(int index){
            return ( index< 0 )? shared.top + 1 + index : index;
        }

        private boolean assertValidIndex(int index){
            if( index > shared.top || 0 > index){
                if(THROW_ON_ERR){
                    throw new ArrayIndexOutOfBoundsException(
                            String.format("index %d out of bounds; available index 0 through %d", index, shared.top)
                    );
                }
                return false;
            }
            return true;
        }
        private boolean assertValidRange(int lo, int hi){
            if(lo >= hi){
                if(THROW_ON_ERR){
                    throw new ArrayIndexOutOfBoundsException(
                            String.format("Low index %d >= high index %d", lo, hi)
                    );
                }
                return false;
            }
            return true;
        }

        private void seekFront( int index ){
            for (
                    curr = shared.head, row = 0;
                    row < index;
                    curr = curr.getNext(), row++
            );
        }

        private void seekBack( int index ){
            for (
                    curr = shared.tail, row = shared.top;
                    row > index;
                    curr = curr.getPrev(), row--
            );
        }

        private boolean seek( int index ){
            index = this.fixNegIndex(index);
            if(this.assertValidIndex(index)){
                if(curr == null){
                    seekFront(index);
                }
                else if(index == row){
                    // Pass; already there
                    return true;
                }
                else if( index == row +1 ){
                    row = index;
                    curr = curr.getNext();
                }
                else if( index == row - 1 ){
                    row = index;
                    curr = curr.getPrev();
                }
                else if( (shared.top + 1 - index) < index ){
                    this.seekBack(index);
                }
                else{
                    this.seekFront(index);
                }
                return true;
            }
            return false;
        }

        private boolean seek(IRecursiveNode target){
            curr = shared.head;
            row = 0;
            while(curr != null && !curr.equals(target)){
                row++;
                curr = curr.getNext();
            }
            return curr != null;
        }

        private void incCur(int inc){
            row += inc;
            if(inc < 0){
                for(int i = inc * -1; i > 0 && curr != null; i--){
                    curr = curr.getPrev();
                }
            }
            else{
                for(int i = inc; i > 0 && curr != null; i--){
                    curr = curr.getNext();
                }
            }
        }

        @Override
        public final void onSizeChanged(){
            /* keep access valid */
            row = 0;
            curr = shared.head;
        }

        @Override
        public String toString() {
            return "  AccessUtil{" +
                    "\n    THROW_ON_ERR: " + THROW_ON_ERR +
                    "\n    curr: " + curr +
                    "\n    row: " + row +
                    "\n  }";
        }
    }

    private static class KeyUtil{// generate same key, whether called before or after next()
        private final ItrBase itr;
        int offset;

        private KeyUtil(ItrBase itr) {
            this.itr = itr;
        }

        public void rewind() {
            offset = 0;
        }

        public void hasNext() {
            offset = 0;
        }

        public void next() {
            offset += itr.inc;
        }

        public int key() {
            return itr.itrAccess.row - offset;
        }

        @Override
        public String toString() {
            return "KeyUtil{offset=" + offset + '}';
        }
    }

    public static abstract class ItrBase implements ListItr, SimpleListener{
        protected final SharedState shared;       // Shares head, tail, top with parent list
        protected final AccessUtil itrAccess;   // Does not share access state with parent list
        protected final KeyUtil keyUtil;
        protected int start, end, inc;
        protected boolean sizeChanged;
        protected boolean busy;

        public ItrBase(SharedState shared) {
            this.shared = shared;
            this.itrAccess = new AccessUtil(shared);
            this.keyUtil = new KeyUtil(this);
            inc = 1;
            start = 0;
            end = 0;
            sizeChanged = true;
        }

        @Override
        public void setRange(int start, int end){
            this.setRange(start, end, 1);
        }

        @Override
        public void setRange(int start, int end, int increment){
            start = itrAccess.fixNegIndex(start);
            end =   itrAccess.fixNegIndex(end);
            if(end < start){
                throw new IllegalStateException(
                        String.format("end < start? parameters passed: start = %d, end = %d", start, end)
                );
            }
            this.start = Math.max(start, 0);
            this.end =   Math.min(end, shared.top);
            this.inc =  increment;
            this.sizeChanged = false;// to preserve settings on rewind
        }

        @Override
        public void clearRange(){
            start = 0;
            end = shared.top;
            inc = 1;
            sizeChanged = false;// to preserve settings on rewind
        }

        @Override
        public void iterateBack() {
            start = 0;
            end = shared.top;
            inc = -1;
            sizeChanged = false;// to preserve settings on rewind
        }

        @Override
        public void setBusy(){
            busy = true;
        }
        @Override
        public void clearBusy(){
            busy = false;
        }

        /* Iterator: manage pointer */
        protected void incCur(){
            itrAccess.incCur(inc);
        }

        @Override
        public void rewind(){
            if(sizeChanged){// clears itr range if size changed
                clearRange();
            }
            itrAccess.seek((inc < 0)? end : start);
            this.setBusy();
            this.keyUtil.rewind();
        }

        /* Iterator index: always correct */
        @Override
        public int key(){
            return this.keyUtil.key();
        }

        @Override
        public int level() {
            return 0;
        }

        @Override
        public boolean hasNext() {
            this.keyUtil.hasNext();
            if(start <= itrAccess.row && itrAccess.row <= end){
                return true;
            }
            else{
                clearBusy();
                return false;
            }
        }

        @Override
        public IRecursiveNode next() {
            this.keyUtil.next();
            IRecursiveNode out = itrAccess.curr;
            incCur();
            return out;
        }

        @Override
        public IRecursiveNode peekNext() {
            IRecursiveNode temp = itrAccess.curr;
            for (int i = 1; i < inc; i++){
                if(temp == null || (temp = temp.getNext()) == null){
                    return null;
                }
            }
            return temp;
        }

        @Override
        public void onSizeChanged(){
            if(busy){
                throw new ConcurrentModificationException(
                    "Only obtain or rewind iterator immediately before loop; never modify list while iterating");
            }
            itrAccess.onSizeChanged();
            this.sizeChanged = true;
        }

        @Override
        public String toString() {
            return "  "+ this.getClass().getSimpleName() + "{" +
                    "\n    keyUtil: " + keyUtil +
                    "\n    busy: " + busy +
                    "\n    start: " + start +
                    "\n    end: " + end +
                    "\n    inc: " + inc +
                    "\n    sizeChanged: " + sizeChanged +
                    "\n  }";
        }
    }

    public static class FlatItr extends ItrBase {
        public FlatItr(SharedState shared) {
            super(shared);
        }
    }

    public static class DepthFirstItr extends ItrBase {
        private ListItr currItr;
        public DepthFirstItr(SharedState shared) {
            super(shared);
            currItr = this;
        }

        @Override
        public void rewind() {
            super.rewind();
            keyUtil.rewind();
            currItr = this;
        }

        @Override
        public int key(){
            // A lot of code to guarantee key and level are always correct
            // Or, use the commented line below and only call key() and level() after next()
            if(this.equals(currItr)){
                IRecursiveNode peek;
                if(keyUtil.offset == 0 && (peek = super.peekNext()) != null && peek.isRecursive()){
                    return 0;
                }
                else{
                    return this.keyUtil.key();
                }
            }
            return currItr.key();
            //return (this.equals(currItr))? this.keyUtil.key() : currItr.key();
        }

        @Override
        public boolean hasNext() {
            keyUtil.hasNext();
            if(this.equals(currItr)){
                return (start <= itrAccess.row && itrAccess.row <= end);
            }
            else if(currItr.hasNext()){
                return true;
            }
            else{
                currItr = this;
                return (start <= itrAccess.row && itrAccess.row <= end);
            }
        }

        @Override
        public IRecursiveNode next() {
            if(this.equals(currItr)){
                IRecursiveNode out = super.next();
                if(out.isRecursive() && out.size() > 0){
                    currItr = out.getDepthFirstIterator();
                    if(inc < 0){
                        currItr.iterateBack();
                    }
                    return currItr.next();
                }
                else{
                    return out;
                }
            }
            else{
                return currItr.next();
            }
        }

        @Override
        public int level() {
            // A lot of code to guarantee key and level are always correct
            // Or, use the single line below and only call key() and level() after next()
            if(this.equals(currItr)){
                IRecursiveNode peek;
                if(keyUtil.offset == 0 && (peek = super.peekNext()) != null && peek.isRecursive()){
                    return shared.level + 1;
                }
                else{
                    return shared.level;
                }
            }
            return currItr.level();
            //return (this.equals(currItr))? shared.level : currItr.level();
        }

        @Override
        public void onSizeChanged() {
            super.onSizeChanged();
            currItr = this;
        }
    }

    @Override
    public ListItr getFlatIterator(){
        if(flatItr == null){
            flatItr = new FlatItr(shared);
            listeners[1] = flatItr;
        }
        flatItr.clearRange();
        flatItr.rewind();
        return flatItr;
    }

    @Override
    public ListItr getDepthFirstIterator() {
        if(depthFirstItr == null){
            depthFirstItr = new DepthFirstItr(shared);
            listeners[2] = depthFirstItr;
        }
        depthFirstItr.clearRange();
        depthFirstItr.rewind();
        return depthFirstItr;
    }

    @Override
    public void disp(){
        System.out.println("=======================\nDisplay ItrList:");
        this.disp(0);
        System.out.println("\nEnd display ItrList\n=======================");
    }

    @Override
    public void disp(int indent){
        String tab = indent == 0?
                "" : new String(new char[4 * indent]).replace('\0', ' ');
        this.getFlatIterator();
        int i = 0;
        while(flatItr.hasNext()){
            IRecursiveNode node = flatItr.next();
            if(node.isRecursive()){
                System.out.println(tab + flatItr.key() + ": ");
                node.getChildList().disp(indent+1);
            }
            else{
                System.out.println(tab + flatItr.key() + ": " + node);
            }
        }
    }

    @Override
    public String toString() {
        IRecursiveNode[] nodes = this.toFlatArray();
        String[] out = new String[nodes.length];
        for(int i = 0; i<nodes.length; i++){
            out[i] = nodes[i].toString();
        }

        return this.getClass().getSimpleName() + "{" +
                "\n" + shared +
                "\n" + access +
                "\n" + ((flatItr == null)? "    flatItr:       unused" : flatItr) +
                "\n" + ((depthFirstItr == null)? "    depthFirstItr: unused" : depthFirstItr) +
                "\n    Content: " +
                "\n    " + String.join(", ", out) +
                "\n}";
    }

    /* =====Public: Access and change the list======================================================================= */

    @Override
    public int size(){
        return shared.top + 1;
    }

    @Override
    public int indexOf(IRecursiveNode target) {
        return (shared.top >= 0 && access.seek(target))? access.row : -1;
    }


    @Override
    public boolean seek( int index ){
        return access.seek(index);
    }

    @Override
    public boolean seek(IRecursiveNode target) {
        return shared.top >= 0 && access.seek(target);
    }


    @Override
    public IRecursiveNode peekFront() {
        return shared.head;
    }

    @Override
    public IRecursiveNode peekBack() {
        return shared.tail;
    }

    @Override
    public IRecursiveNode peekIn( int index ) {
        return (this.seek(index))?
                access.curr : null;
    }

    @Override
    public IRecursiveList peekFront(int hi) {
        IRecursiveList out = newList();
        IRecursiveNode curr = shared.head;
        while(hi >= 0 && curr != null){
            out.pushBack(curr.copy());
            hi--;
            curr = curr.getNext();
        }
        return out;
    }

    @Override
    public IRecursiveList peekBack(int lo) {
        IRecursiveList out = newList();
        IRecursiveNode curr = this.peekIn(lo);
        while(curr != null){
            out.pushBack(curr.copy());
            curr = curr.getNext();
        }
        return out;
    }

    @Override
    public IRecursiveList peekIn(int lo, int hi) {
        IRecursiveList out = newList();

        if(shared.top >= 0 && access.assertValidRange(lo, hi)){
            this.getFlatIterator();
            flatItr.setRange(lo, hi, 1);
            flatItr.rewind();
            while(flatItr.hasNext()){
                out.pushBack(flatItr.next().copy());
            }
            out.peekFront().setPrev(null);
            out.peekBack().setNext(null);
        }
        return out;
    }

    @Override
    public IRecursiveList peekFront(IRecursiveNode last) {
        IRecursiveList out = newList();
        IRecursiveNode first = shared.head;
        if(shared.top >= 0){
            while(first != null && !first.equals(last)){
                out.pushBack(first.copy());
                first = first.getNext();
            }
            if(first != null){
                out.pushBack(first.copy());
            }
        }
        return out;
    }

    @Override
    public IRecursiveList peekBack(IRecursiveNode first) {
        IRecursiveList out = newList();
        if(shared.top >= 0 && this.seek(first)){
            while(first != null){
                out.pushBack(first.copy());
                first = first.getNext();
            }
        }
        return out;
    }

    @Override
    public IRecursiveList peekIn(IRecursiveNode first, IRecursiveNode last) {
        IRecursiveList out = newList();

        if(shared.top >= 0 && this.seek(first) &&  this.seek(last)){
            do{
                out.pushBack(first.copy());
                first = first.getNext();
            }
            while(first != null && !first.equals(last));

            if(first != null){
                out.pushBack(first.copy());
            }
        }
        return out;
    }

    @Override
    public void pushFront(IRecursiveNode newHead){
        newHead.setLevel(shared.level + 1);
        newHead.setNext(shared.head);
        newHead.setPrev(null);
        if(shared.top < 0){   // empty list
            shared.tail = newHead;
        }
        shared.head = newHead;
        shared.incSize();
    }

    @Override
    public void pushBack (IRecursiveNode newTail){
        if(shared.top < 0){
            this.pushFront(newTail);
        }
        else{
            newTail.setLevel(shared.level + 1);
            newTail.setPrev(shared.tail);
            newTail.setNext(null);
            shared.tail.setNext(newTail);
            shared.tail = newTail;
            shared.incSize();
        }
    }

    @Override
    public void pushIn(int index, IRecursiveNode newNode){
        if(this.seek(index)){
            newNode.setLevel(shared.level + 1);
            newNode.setPrev(access.curr.getPrev());
            newNode.getPrev().setNext(newNode);
            newNode.setNext(access.curr);
            access.curr.setPrev(newNode);
            access.curr = newNode;
            access.row++;
            shared.incSize();
        }
    }


    @Override
    public void pushFront(IRecursiveList newFront) {
        if(shared.top >= 0){
            IRecursiveNode newFrontTail = newFront.peekBack();
            newFrontTail.setNext(shared.head);
            shared.head.setPrev(newFrontTail);
            shared.head = newFront.peekFront();
            shared.incSize(newFront.size());
        }
        else{
            pushBack(newFront);
        }
    }

    @Override
    public void pushBack(IRecursiveList newBack) {
        if(shared.top >= 0){
            IRecursiveNode newBackHead = newBack.peekFront();
            newBackHead.setPrev(shared.tail);
            shared.tail.setNext(newBackHead);
        }
        else{
            shared.head = newBack.peekFront();
        }
        shared.tail = newBack.peekBack();
        shared.incSize(newBack.size());
    }

    @Override
    public void pushIn(int index, IRecursiveList newIn) {
        if(shared.top >= 0){
            index = access.fixNegIndex(index);

            if(index == 0){
                pushFront(newIn);
            }
            else if(index > shared.top){
                pushBack(newIn);
            }
            else{
                IRecursiveNode e = this.peekIn(index);
                IRecursiveNode s = e.getPrev();

                IRecursiveNode newInHead = newIn.peekFront();
                newInHead.setPrev(s);
                s.setNext(newInHead);

                IRecursiveNode newInTail = newIn.peekBack();
                newInTail.setNext(e);
                e.setPrev(newInTail);

                shared.incSize(newIn.size());
            }
        }
        else{
            pushBack(newIn);
        }
    }


    @Override
    public IRecursiveNode popFront(){
        if( shared.top >= 0 ){
            IRecursiveNode victim = shared.head;
            shared.head = victim.getNext();
            shared.head.setPrev(null);
            shared.decSize();
            return victim;
        }
        else{
            return null;
        }
    }

    @Override
    public IRecursiveNode popBack(){
        if( shared.top >= 0 ){
            IRecursiveNode victim = shared.tail;
            shared.tail = victim.getPrev();
            shared.tail.setNext(null);
            shared.decSize();
            return victim;
        }
        else{
            return null;
        }
    }

    @Override
    public IRecursiveNode popIn( int index) {
        if( index == 0 ){
            return this.popFront();
        }
        else if( index == shared.top ){
            return this.popBack();
        }
        else if(access.seek( index )){
            IRecursiveNode victim = access.curr;
            IRecursiveNode prev = access.curr.getPrev();
            IRecursiveNode next = access.curr.getNext();
            prev.setNext(next);
            next.setPrev(prev);
            access.curr = next;
            shared.decSize();
            return victim;
        }
        else{
            return null;
        }
    }


    @Override
    public IRecursiveList popFront(int hi) {
        IRecursiveList sublist = peekIn(0, hi);
        removeFront(hi);
        return sublist;
    }

    @Override
    public IRecursiveList popBack(int lo) {
        IRecursiveList sublist = peekIn(lo, shared.top);
        removeBack(lo);
        return sublist;
    }

    @Override
    public IRecursiveList popIn(int lo, int hi) {
        IRecursiveList sublist = peekIn(lo, hi);
        removeIn(lo, hi);
        return sublist;
    }


    @Override
    public void removeFront(int hi) {
        hi = access.fixNegIndex(hi);

        shared.head = peekIn(hi).getNext();
        shared.head.setPrev(null);

        shared.decSize(1 + hi);
    }

    @Override
    public void removeBack(int lo) {
        lo = access.fixNegIndex(lo);

        shared.tail = peekIn(lo).getPrev();
        shared.tail.setNext(null);

        shared.decSize(1 + shared.top - lo);
    }

    @Override
    public void removeIn(int lo, int hi) {
        lo = access.fixNegIndex(lo);
        hi = access.fixNegIndex(hi);

        if(lo == 0){
            removeFront(hi);
        }
        else if(hi == shared.top){
            removeBack(lo);
        }
        else{
            IRecursiveNode s = peekIn(lo);
            IRecursiveNode e = peekIn(hi);
            s.getPrev().setNext(e.getNext());
            e.getNext().setPrev(s.getPrev());

            shared.decSize(1 + hi - lo);
        }
    }

    @Override
    public void setLevel(int level) {
        shared.level = level;
    }

    @Override
    public int getLevel() {
        return shared.level;
    }

    /* =====Access to tree branches, if recursive==================================================================== */

    @Override
    public void pushBelow(IRecursiveList childList, int... params) {
        IRecursiveNode target = this.peekIn(params[shared.level]);
        if(shared.level == params.length - 1){
            childList.setLevel(shared.level + 1);
            target.setChildList(childList);
        }
        else{
            target.pushBelow(childList, params);
        }
    }

    @Override
    public IRecursiveList popBelow(int... params) {
        IRecursiveNode target = peekIn(params[shared.level]);
        if(shared.level == params.length - 1){
            return target.getChildList();// may be null
        }
        else if(target.isRecursive()){
            return target.popBelow(params);
        }
        return null;
    }

    /* =====Multi-node manipulation, slicing and sub-listing: by index or by object================================== */

    @Override
    public IRecursiveList newList() {
        return new RecursiveList();
    }

    @Override
    public IRecursiveList copy(){
        IRecursiveList out = newList();

        if(shared.top >= 0){
            IRecursiveNode s = shared.head;
            do{
                out.pushBack(s.copy());
                s = s.getNext();
            }
            while(s != null);
        }
        return out;
    }

    @Override
    public IRecursiveList reverse() {
        IRecursiveList out = newList();

        if(shared.top >= 0){
            IRecursiveNode s = shared.tail;
            do{
                out.pushBack(s.copy());
                s = s.getPrev();
            }
            while(s != null);
        }
        return out;
    }


    @Override
    public ArrayList<IRecursiveNode> toFlatArrayList() {
        ArrayList<IRecursiveNode> out = new ArrayList<>();

        if(shared.top >= 0){
            IRecursiveNode s = shared.head;
            do{
                out.add(s);
                s = s.getNext();
            }
            while(s != null);
        }
        return out;
    }

    @Override
    public IRecursiveNode[] toFlatArray() {
        IRecursiveNode[] out = new IRecursiveNode[shared.top+1];

        if(shared.top >= 0){
            IRecursiveNode s = shared.head;
            int i = 0;
            do{
                out[i++] = s;
                s = s.getNext();
            }
            while(s != null);

        }
        return out;
    }

    @Override
    public IRecursiveNode[][][] toBreadthFirstArray() {
        if(shared.top >= 0){
            int maxLevel = this.refreshLevels(0);

            IRecursiveNode[][][] mainOut = new IRecursiveNode[maxLevel+1][][];
            mainOut[0] = new IRecursiveNode[1][];
            toBreadthFirstArray(this, mainOut, 0, 0);
            return mainOut;
        }
        return new IRecursiveNode[0][][];
    }

    protected final void toBreadthFirstArray(IRecursiveList mainIn, IRecursiveNode[][][] mainOut, int i, int j) {
        mainOut[i][j] = new IRecursiveNode[mainIn.size()];
        int k;

        ArrayList<IRecursiveNode> recursiveNodes = new ArrayList<>();
        IRecursiveNode s = mainIn.peekFront();
        k = 0;
        do{
            mainOut[i][j][k] = s;
            if(s.isRecursive() && s.size() > 0){
                recursiveNodes.add(s);
            }
            s = s.getNext();
            k++;
        }
        while(s != null);

        if(!recursiveNodes.isEmpty()){
            mainOut[i+1] = new IRecursiveNode[recursiveNodes.size()][];
            int j0 = 0;
            for(IRecursiveNode recursiveNode : recursiveNodes){
                IRecursiveList childList = recursiveNode.getChildList();
                this.toBreadthFirstArray(childList, mainOut, i + 1, j0);
                j0++;
            }
        }
    }

    @Override
    public final int refreshLevels(int setLevel){
        this.setLevel(setLevel);
        int maxLevel = setLevel;
        if(shared.top >= 0){
            IRecursiveNode s = shared.head;
            int i = 0;
            do{
                if(s.isRecursive()){
                    maxLevel = Math.max(maxLevel, s.refreshLevels(setLevel + 1));
                }
                s = s.getNext();
                i++;
            }
            while(s != null);
        }
        return maxLevel;
    }
}
