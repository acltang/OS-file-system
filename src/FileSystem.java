public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    
    public FileSystem(int diskBlocks) {
        //create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);
        
        //create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.inodeBlocks);
        
        //file table is created, and store directory in the file table
        filetable = new FileTable(directory);
        
        //directory reconstruction
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if(dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close(dirEnt);
    }
    
    void sync() {
        FileTableEntry entry = open("/", "w");
        byte[] directoryBuf = directory.directory2bytes();
        write(entry, directoryBuf);
        close(entry);
        superblock.sync();
    }
    
    /*
     format()
     format the file system
     */
    boolean format(int files) {
        if(filetable.fempty() == false) {
            return false;
        }
        superblock.format(files);
        directory = new  Directory(superblock.totalInodes);
        filetable = new FileTable(directory, superblock.totalInodes);
        return true;
    }
    
    /*
     open()
     */
    FileTableEntry open(String filename, String mode) {
        FileTableEntry entry = filetable.falloc(filename, mode);
        if (mode.equals("w") && entry != null && deallocAllBlocks(entry) == false) {
            return null;
        }
        return entry;
    }
    
    public synchronized boolean close(FileTableEntry ftEnt) {
        ftEnt.count--;
        if (ftEnt.count > 0) return true;
        return filetable.ffree(ftEnt);
    }
    
    int fsize(FileTableEntry ftEnt) {
        if (ftEnt == null || ftEnt.inode == null) { return -1; }
        return ftEnt.inode.length;
    }
    
    int read(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt == null) { return -1; }
        
        synchronized (ftEnt) {
            int bufferLeft = buffer.length;
            int byteRead = 0;
            int fileLength = fsize(ftEnt);
            
            while (ftEnt.seekPtr < fileLength && bufferLeft > 0) {
                
            }
        }
    }
    
    int write(FileTableEntry ftEnt, byte[] buffer) {
        
    }
    
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        
    }
    
    boolean delete(String filename) {
        
    }
    
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
    
    int seek(FileTableEntry ftEnt, int offset, int whence) {
        
    }
}
