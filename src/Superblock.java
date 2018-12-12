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

public class SuperBlock {
    public int totalBlocks;     // the number of disk blocks
    public int totalInodes;    // the number of inodes
    public int freeList;       // the block number of the free list's head
    private final static int defaultInodeBlocks = 64;
    
    public SuperBlock(int diskSize) {
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
        byte[] tempBlock = new byte[Disk.blockSize];
        
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

    public int getFreeBlock() {
        int tempList = freeList;
        if (tempList == -1 || tempList > totalBlocks) {
            return -1;
        }

        byte[] block = new byte[Disk.blockSize];
        SysLib.rawread(tempList, block);
        freeList = SysLib.bytes2int(block, 0);
        SysLib.int2bytes(0, block, 0);
        SysLib.rawwrite(tempList, block);
        return tempList;
    }

    public boolean returnBlock(int block) {
        if (block < 0 || block > totalBlocks) {
            return false;
        }
        byte[] retBlock = new byte[Disk.blockSize];
        for (int i = 0; i < Disk.blockSize; i++) {
            retBlock[i] = 0;
        }
        SysLib.int2bytes(freeList, retBlock, 0);
        SysLib.rawwrite(block, retBlock);
        freeList = block;
        return true;
    }
}
