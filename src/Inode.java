
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer
    
    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }
    
    /*
    Inode()
    retrieve an inode from disk, read it and collect the Inode information such as
    file size, number of file-table entries, flag, direct pointers and indirect
    pointer
    */
    Inode(short iNumber) {
        byte[] iInfo = new byte[Disk.blockSize];
        int iBlock = (iNumber / 16) + 1;         // location of Inode in Disk blocks
        int offset = (iNumber % 16) * iNodeSize; // specific location of Inode in a block
        
        SysLib.rawread(iBlock, iInfo);
        length = SysLib.bytes2int(iInfo, offset);
        offset += 4;                             // integer is 4 bytes in size
        count = (short)SysLib.bytes2int(iInfo, offset);
        offset += 2;                             // short is 2 bytes in size
        flag = (short)SysLib.bytes2int(iInfo, offset);
        offset += 2;                             // short is 2 bytes in size
        
        for (int i = 0; i < directSize; i++) {   // total 11 direct pointers
            direct[i] = SysLib.bytes2short(iInfo, offset);
            offset += 2;                         // each short pointer is 2 bytes in size
        }
        indirect = SysLib.bytes2short(iInfo, offset); // One single indirect pointer
        offset += 2;
    }
    
    /*
    toDisk()
    save to disk as the i-th inode
    */
    int toDisk(short iNumber) {
        byte[] iInfo = new byte[Disk.blockSize];
        int iBlock = (iNumber / 16) + 1;         // location of Inode in Disk blocks
        int offset = (iNumber % 16) * iNodeSize; // specific location of Inode in a block
        
        SysLib.int2bytes(length, iInfo, offset); // Put the length info to buffer
        offset += 4;
        SysLib.short2bytes(count, iInfo, offset);  // Put the count info to buffer
        offset += 2;
        SysLib.short2bytes(flag, iInfo, offset);   // Put the flag info to buffer
        offset += 2;
        
        for (int i = 0; i < directSize; i++) {   // Put 11 direct pointers info to buffer
            SysLib.short2bytes(direct[i], iInfo, offset);
            offset += 2;
        }
        SysLib.short2bytes(indirect, iInfo, offset); // Put indirect pointer info to buf
        offset += 2;
        SysLib.rawwrite(iBlock, iInfo); // Write iNode info back to the disk
    }
    
    /*
    findDataBlock()
    find the specific location of data block where direct / indirect pointers are
    heading to
    */
    public short findDataBlock(int offset) {
        byte[] indirectBuf = new byte[Disk.blockSize];
        int dirLocation = offset / Disk.blockSize;
        if (dirLocation < directSize) {
            return direct[dirLocation];
        }
        else if (indirect >= 0) {
            SysLib.rawread(indirect, indirectBuf);
            int indirLocation = dirLocation - directSize;
            return SysLib.bytes2short(indirectBuf, indirLocation*2);
        }
        return -1;
    }
    
    public void setDataBlock(int offset, short block){
        byte[] indirectBuf = new byte[Disk.blockSize];
        int dirLocation = offset / Disk.blockSize;
        if (dirLocation < directSize) {
            if (direct[dirLocation] == -1){
                direct[dirLocation] = block;
            }
            else if (direct[dirLocation]
        }
        else if (indirect >= 0) {
           
        }
    }
}

