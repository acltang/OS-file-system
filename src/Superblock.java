/*
Superblock.java
This block includes the necessary metadata of the file system during operation.
For example, It includes the number of disk blocks, the number of inodes, the
pointer to a list of free blocks, etc.
<Functionalities>
-
-
-
*/

public class Superblock {
    public int totalBlocks;     // the number of disk blocks
    public int totalInodes;    // the number of inodes
    public int freeList;       // the block number of the free list's head
    private final static int defaultInodeBlocks = 64;
    
    public Superblock(int diskSize) {
        byte[] superBlock = new byte[Disk.blockSize];    // Disk.blockSize = 512 bytes
        SysLib.rawread(0, superBlock);
        // Convert bytes of block data to integer to read
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);
        
        // Format the disk
        if (totalBlocks != diskSize || totalInodes <= 0 || freeList < 2) {
            totalBlocks = diskSize;     // 1000
            format(defaultInodeBlocks);
        }
    }
    
    /*
    sync()
    To sync the contents of the superblock to disk, convert all necessary data
    to byte format and write it to disk
    */
    public void sync() {
        byte[] tempBlock = new byte[Disk.blockSIze];
        
        SysLib.int2bytes(totalBlocks, tempBlock, 0);
        SysLib.int2bytes(totalInodes, tempBlock, 4);
        SysLib.int2bytes(freeList, tempBlock, 8);
        SysLib.rawwrite(0, tempBlock);
        //SysLib.cerr("Superblock synchronized\n");
    }
    
    /*
    format()
    Format the disk
    */
    public void format(int inodesNum) {
        totalInodes = inodesNum;
        for (short i = 0; i < totalInodes; i++) {
            Inode newInode = new Inode();
            newInode.toDisk(i); //
        }
        freeList = (totalInodes / 16) + 1;  // totalInodes(64) / 16(total inodes for a signle block) + 1 = block number of freeList's head 
        // To go through every single free block and initialize it 
        for (int i = freeList; i < totalBlocks; i++) {
            byte[] emptyBlock = new byte[Disk.blockSize];
            for (int j = 0; j < 512; j++) { emptyBlock[j] = 0; }
            SysLib.int2bytes(i+1, emptyBlock, 0);
            SysLib.rawwrite(i, emptyBlock);
        }
        sync();
    }
}
