public class FileTable {

    public class FileTableEntry {          // Each table entry should have
        public int seekPtr;                 //    a file seek pointer
        public final Inode inode;           //    a reference to its inode
        public final short iNumber;         //    this inode number
        public int count;                   //    # threads sharing this entry
        public final String mode;           //    "r", "w", "w+", or "a"
        public FileTableEntry ( Inode i, short inumber, String m ) {
            seekPtr = 0;             // the seek pointer is set to the file top
            inode = i;
            iNumber = inumber;
            count = 1;               // at least on thread is using this entry
            mode = m;                // once access mode is set, it never changes
            if ( mode.compareTo( "a" ) == 0 ) // if mode is append,
                seekPtr = inode.length;        // seekPtr points to the end of file
        }
    }

    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}