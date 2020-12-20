# Recursive-List
The mother of all data structures   
* Created as part of the Semantic Analyzer repository. 
* I needed a list that could recurse on its own nodes. It's a tree, but visually more a series of linked arcs, like a fancy necklace.
* List nodes also implement RecursiveList interface, so each node can be its own linked list
* Complicated because it includes every conceivable access type, iteration and expansion.
* See code comments and Semantic Analyzer tests for more usage examples
## List Access
* Access top-level members with push, pop, peek: from front, back and middle
* Access top-level sub-lists with push, pop, peek: from front, back and middle: 1 extra parameter for range
* Supports negative indexing for all top-level access methods: -1 for last item
## Iterators
* Flat iterator parses top level only
* Depth-first iterator parses all, skipping the branch nodes: disp() for example
## Conversion
* Get list as flat array (top level) or 3-dimensional breadth-first array: all nodes including branches
## Payload
* RecursiveNode is abstract with an Object payload. Extend to concrete class to use.
* Copy constructor preserves payload. RecursiveNode overrides Object.equals() so copied nodes test equal. 
## Usage  
* Create and populate   

        String[] content = text.split(" ");   
        IRecursiveList list = new RecursiveList();   
        for(String s : content){   
            FlagNode node = new FlagNode(); // extends RecursiveNode   
            node.set(IN, s);                // sets text
            list.pushBack(node);   

* Access   

        System.out.println(list.size());        // 10
        IRecursiveNode first = list.popFront();
        IRecursiveNode middle = list.popIn(5);
        IRecursiveNode last = list.popBack();
        System.out.println(list.size());        // 7
        IRecursiveNode iPeeked = list.peekBack();
        System.out.println(list.size());        // 7
        
* Iterator   

        IRecursiveList.ListItr itr = list.getDepthFirstIterator();
        while(itr.hasNext()){
            System.out.println(itr.level() + ": " + itr.key() + ": " + itr.next());
        }
        
* Iterator, setting range and back-iteration

        IRecursiveList.ListItr itr = list.getFlatIterator();
        itr.setRange(2, -2, -1);  // start 2, end 2nd from last, backward
        itr.rewind();             // applies range settings
        while(itr.hasNext()){
            System.out.println(itr.key() + ": " + itr.next());
        }
        
        
* Equality

        IRecursiveNode s = list.peekIn(2);
        IRecursiveNode e = s.copy();
        System.out.println(s.equals(e)); // true
