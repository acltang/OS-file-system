import java.util.Vector;

public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector<FileTableEntry>( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        Inode inode = null;
        short iNumber;

        if (filename.equals("/")) {
            iNumber = 0;
        }
        else {
            iNumber = dir.namei(filename);
        }

        while (true) {
            if (iNumber < 0) {  // file doesn't exist
                if (mode.equals("r")) {
                    return null;
                }
                else { // w, w+, or a
                    iNumber = dir.ialloc(filename);
                    inode = new Inode();
                    inode.flag = 2;
                    break;
                }
            }
            else { // file exists
                inode = new Inode(iNumber);
                if (mode.equals("r")) {
                    if (inode.flag == 0) {
                        inode.flag = 1; // set used
                        break;
                    }
                }
                else { // w, w+, or a
                    return null;
                }
                try {
                    wait();
                }catch (Exception e){}
                break;
            }
        }

        inode.count++;
        inode.toDisk(iNumber);

        FileTableEntry entry = new FileTableEntry(inode, iNumber, mode);
        table.add(entry);

        return entry;
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
        if (table.remove(e)) {
            e.inode.count--;
            if (e.inode.count == 0) {
                e.inode.flag = 0;
            }
            notifyAll();
            e.inode.toDisk(e.iNumber);
            return true;
        }
        return false;
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}