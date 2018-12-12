public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private final static int ERROR = -1;

    /*
    FileSystem()
    Constructor of FileSystem class
    To initialize the superblock, directory and file-table and also
    reconstruct the directory
    */
    public FileSystem(int diskBlocks) {
        //create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);

        //create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);

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

    /*
    sync()
    To ensure that the superblock gets synchronized
    */
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
        filetable = new FileTable(directory);
        return true;
    }

    /*
    open()
    To access to the File-Table-Entry corresponding to the file name with given mode
    */
    FileTableEntry open(String filename, String mode) {
        FileTableEntry entry = filetable.falloc(filename, mode);
        System.out.println("test");
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

    /*
    fsize()
    To get the total size of file
    */
    int fsize(FileTableEntry ftEnt) {
        if (ftEnt == null || ftEnt.inode == null) { return -1; }
        return ftEnt.inode.length;
    }

    /*
    read()

    */
    int read(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt == null) { return ERROR; }
        if (ftEnt.mode == "a" || ftEnt.mode == "w") { return ERROR; }
        synchronized (ftEnt) {
            int bufferLeft = buffer.length; // Size of total buffer to read in file
            int bufferRead = 0;             // Keep tracking the bytes read
            int fileLength = fsize(ftEnt);  // Size of file
            int offset = ftEnt.seekPtr;     // Keep tracking the block to read by this seek pointer
            // If seek pointer doesn't reach to the end of file and some buffer to read
            // is still left in the file
            while (ftEnt.seekPtr < fileLength && bufferLeft > 0) {
                // To find the specific location of block through the seek pointer
                int blockLocation = ftEnt.inode.findDataBlock(offset);
                if (blockLocation == ERROR) { break; }
                byte[] tempBuffer = new byte[Disk.blockSize];
                // To load the block data from the disk and store in the tempBuffer
                SysLib.rawread(blockLocation, tempBuffer);

                int startingPtr = offset % Disk.blockSize;
                int blockDataLeft = Disk.blockSize - startingPtr;
                int bufSizeToRead = Math.min(bufferLeft, Math.min(blockDataLeft,  fileLength - offset));

                // To copy the data of blocks into the buffer
                System.arraycopy(tempBuffer, startingPtr, buffer, bufferRead, bufSizeToRead);

                // To update the remaining size of buffer after reading the file
                bufferLeft -= bufSizeToRead;
                // To update the reading size of buffer after reading the file
                bufferRead += bufSizeToRead;
                // To update the location of seek pointer after reading the file
                offset += bufSizeToRead;
            }
            return bufferRead;
        }
    }
    
    int write(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt == null || ftEnt.inode == null || ftEnt.mode == "r") {
            return -1;
        }
        synchronized (ftEnt) {
            int length = buffer.length;
            int writeCount = 0;
            while (length > 0) {
                byte[] blockData = new byte[Disk.blockSize];
                int offset = ftEnt.seekPtr % Disk.blockSize;
                int space = Math.min(Disk.blockSize - offset, length);
                int blockNumber = ftEnt.inode.findDataBlock(ftEnt.seekPtr);
                if (blockNumber == -1) {
                    blockNumber = ftEnt.inode.setDataBlock(ftEnt.seekPtr, (short)superblock.getFreeBlock());
                }

                // write to block
                SysLib.rawread(blockNumber, blockData);
                System.arraycopy(buffer, writeCount, blockData, offset, space);
                SysLib.rawwrite(blockNumber, blockData);
                writeCount += space;
                ftEnt.seekPtr += space;
                length -= space;

                // fix inode length
                if(ftEnt.seekPtr > ftEnt.inode.length){
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
            }
            ftEnt.inode.toDisk(ftEnt.iNumber);
            return writeCount;
        }
    }

    /*
    deallocAllBlocks()
    To deallocate all of data blocks existing in the file
    */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        int entryCount = ftEnt.inode.count;
        if (ftEnt == null || entryCount != 1) { return false; }
        byte[] indirectBlock;
        if (ftEnt.inode.indirect == ERROR) {
            indirectBlock = null;
        }
        else {
            indirectBlock = new byte[Disk.blockSize];
            SysLib.rawread(ftEnt.inode.indirect, indirectBlock);
            ftEnt.inode.indirect = -1;
        }
        // To deallocate the indirect data blocks
        if (indirectBlock != null) {
            int offset = 0;
            short dataID = SysLib.bytes2short(indirectBlock, offset);
            while (offset < Disk.blockSize && dataID != ERROR){
                offset = offset + 2;
                superblock.returnBlock(dataID);
                dataID = SysLib.bytes2short(indirectBlock, offset);
            }
        }
        superblock.returnBlock(ftEnt.inode.indirect);

        // To deallocate the direct data blocks(11 direct pointers)
        for (int dirIndex = 0; dirIndex < 11; dirIndex++) {
            if (ftEnt.inode.direct[dirIndex] != ERROR) {
                superblock.returnBlock(ftEnt.inode.direct[dirIndex]);
                ftEnt.inode.direct[dirIndex] = ERROR;
            }
        }
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }
    
    boolean delete(String filename) {
        if(directory.namei(filename) == -1){
            return false;
        }
        return directory.ifree(directory.namei(filename));
    }
    
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
    
    int seek(FileTableEntry ftEnt, int offset, int whence) {
        if (whence < SEEK_SET || whence > SEEK_CUR) {
            return -1;
        }
        switch(whence) {
            case SEEK_SET:
                ftEnt.seekPtr = offset;
                break;
            case SEEK_CUR:
                ftEnt.seekPtr += offset;
                break;
            case SEEK_END:
                ftEnt.seekPtr = ftEnt.inode.length + offset;
                break;
        }

        // fix inode length
        if(ftEnt.seekPtr > ftEnt.inode.length){
            ftEnt.inode.length = ftEnt.seekPtr;
        }
        if (ftEnt.seekPtr < 0) {
            ftEnt.seekPtr = 0;
        }
        return ftEnt.seekPtr;
    }
}
