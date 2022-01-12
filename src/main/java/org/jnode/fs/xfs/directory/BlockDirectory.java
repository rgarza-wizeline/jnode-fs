package org.jnode.fs.xfs.directory;

import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.xfs.XfsEntry;
import org.jnode.fs.xfs.XfsFileSystem;
import org.jnode.fs.xfs.XfsObject;
import org.jnode.fs.xfs.inode.INode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * A XFS block directory inode.
 *
 * When the shortform directory space exceeds the space in an inode, the
 * directory data is moved into a new single directory block outside the inode.
 * The inode’s format is changed from “local” to “extent”
 *
 * @author Ricardo Garza
 * @author Julio Parra
 */

public class BlockDirectory extends XfsObject  {

    /**
     * The logger implementation.
     */
    private static final Logger log = LoggerFactory.getLogger(BlockDirectory.class);

    /**
     * The magic number XD2B on < v5 filesystem
     */
    private static final long MAGIC_V4 = asciiToHex("XD2B");

    /**
     * The magic number XDB3 on a v5 filesystem
     */
    private static final long MAGIC_V5 = asciiToHex("XDB3");

    /**
     * The offset of the first entry version 4
     */
    public final static int V4_LENGTH = 16;

    /**
     * The offset of the first entry version 5
     */
    public final static int V5_LENGTH = 64;

    /**
     * The filesystem
     */
    XfsFileSystem fs;

    /**
     *  Creates a new block directory entry.
     *
     *  @param data the data.
     *  @param offset the offset.
     *  @param fs the filesystem instance.
     */
    public BlockDirectory(byte[] data, int offset, XfsFileSystem fs) throws IOException {
        super(data, offset);

        if ((getMagicSignature() != MAGIC_V5) && (getMagicSignature() != MAGIC_V4)) {
            throw new IOException("Wrong magic number for XFS: " + getAsciiSignature(getMagicSignature()));
        }
        this.fs = fs;
    }

    /**
     * Gets the Checksum of the directory block.
     *
     * @return the Checksum
     */
    public long getChecksum() {
        return getUInt32(4);
    }

    /**
     * Gets the Block number of this directory block.
     *
     * @return the Block number
     */
    public long getBlockNumber() {
        return getInt64(8);
    }

    /**
     * Gets the log sequence number of the last write to this block.
     *
     * @return the log sequence number
     */
    public long getLogSequenceNumber() {
        return getInt64(16);
    }

    /**
     * Gets the UUID of this block
     *
     * @return the UUID
     */
    public String getUuid() {
        return readUuid(24);
    }

    /**
     * Gets the inode number that this directory block belongs to
     *
     * @return the parent inode
     */
    public long getParentInode() {
        return getInt64(40);
    }

    /**
     * Get the inode's entries
     *
     * @return a list of inode entries
     */
    public List<FSEntry> getEntries(FSDirectory parentDirectory) throws IOException {
        long offset = getOffset() + V5_LENGTH;
        List<FSEntry> data = new ArrayList<>(10);
        int i = 0;
        while (true) {
            final BlockDirectoryEntry entry = new BlockDirectoryEntry(getData(), offset, fs);
            if (entry.getNameSize() == 0) {
                break;
            }
            INode iNode = fs.getINode(entry.getINodeNumber());
            data.add(new XfsEntry(iNode, entry.getName(), i++, fs, parentDirectory));
            offset += entry.getOffsetSize();
        }
        return data;
    }
}

