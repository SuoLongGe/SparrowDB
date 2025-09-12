import java.util.ArrayList;
import java.util.List;

/**
 * B+ Tree Node Abstract Base Class
 */
public abstract class BPlusTreeNode {
    protected int pageId; // Node corresponding page ID
    protected boolean isLeaf; // Whether it is a leaf node
    protected int parentPageId; // Parent node page ID
    protected List<BPlusTreeKey> keys; // Key list
    protected int maxKeys; // Maximum number of keys
    
    // Page layout constants
    protected static final int HEADER_SIZE = 16; // Header information size (page ID + is leaf + parent page ID + key count)
    protected static final int PAGE_ID_SIZE = 4;
    protected static final int BOOLEAN_SIZE = 1;
    protected static final int INT_SIZE = 4;
    
    public BPlusTreeNode(int pageId, boolean isLeaf, int maxKeys) {
        this.pageId = pageId;
        this.isLeaf = isLeaf;
        this.parentPageId = -1;
        this.keys = new ArrayList<>();
        this.maxKeys = maxKeys;
    }
    
    // Getters and Setters
    public int getPageId() { return pageId; }
    public void setPageId(int pageId) { this.pageId = pageId; }
    
    public boolean isLeaf() { return isLeaf; }
    public void setLeaf(boolean isLeaf) { this.isLeaf = isLeaf; }
    
    public int getParentPageId() { return parentPageId; }
    public void setParentPageId(int parentPageId) { this.parentPageId = parentPageId; }
    
    public List<BPlusTreeKey> getKeys() { return keys; }
    public void setKeys(List<BPlusTreeKey> keys) { this.keys = keys; }
    
    public int getMaxKeys() { return maxKeys; }
    public void setMaxKeys(int maxKeys) { this.maxKeys = maxKeys; }
    
    public int getKeyCount() { return keys.size(); }
    
    public boolean isFull() { return keys.size() >= maxKeys; }
    public boolean isUnderflow() { return keys.size() < (maxKeys + 1) / 2; }
    
    /**
     * Add key
     */
    public void addKey(BPlusTreeKey key) {
        keys.add(key);
    }
    
    /**
     * Insert key at specified position
     */
    public void insertKey(int index, BPlusTreeKey key) {
        keys.add(index, key);
    }
    
    /**
     * Remove key at specified position
     */
    public BPlusTreeKey removeKey(int index) {
        return keys.remove(index);
    }
    
    /**
     * Get key at specified position
     */
    public BPlusTreeKey getKey(int index) {
        return keys.get(index);
    }
    
    /**
     * Find key position, return insertion position if not found
     */
    public int findKeyPosition(BPlusTreeKey key) {
        int left = 0, right = keys.size();
        while (left < right) {
            int mid = (left + right) / 2;
            if (keys.get(mid).compareTo(key) < 0) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }
    
    /**
     * Check if key exists
     */
    public boolean containsKey(BPlusTreeKey key) {
        int pos = findKeyPosition(key);
        return pos < keys.size() && keys.get(pos).equals(key);
    }
    
    /**
     * Serialize node to page
     */
    public abstract void serializeToPage(Page page);
    
    /**
     * Deserialize node from page
     */
    public abstract void deserializeFromPage(Page page);
    
    /**
     * Calculate bytes occupied by node in page
     */
    public abstract int getSerializedSize();
    
    /**
     * Clear node
     */
    public void clear() {
        keys.clear();
        parentPageId = -1;
    }
    
    @Override
    public String toString() {
        return String.format("BPlusTreeNode{pageId=%d, isLeaf=%s, keyCount=%d, keys=%s}", 
                           pageId, isLeaf, keys.size(), keys);
    }
}

/**
 * B+ Tree Internal Node
 */
class BPlusTreeInternalNode extends BPlusTreeNode {
    private List<Integer> childPageIds; // Child node page ID list
    
    public BPlusTreeInternalNode(int pageId, int maxKeys) {
        super(pageId, false, maxKeys);
        this.childPageIds = new ArrayList<>();
    }
    
    public List<Integer> getChildPageIds() { return childPageIds; }
    public void setChildPageIds(List<Integer> childPageIds) { this.childPageIds = childPageIds; }
    
    public int getChildCount() { return childPageIds.size(); }
    
    public void addChild(int childPageId) {
        childPageIds.add(childPageId);
    }
    
    public void insertChild(int index, int childPageId) {
        childPageIds.add(index, childPageId);
    }
    
    public int removeChild(int index) {
        return childPageIds.remove(index);
    }
    
    public int getChild(int index) {
        return childPageIds.get(index);
    }
    
    /**
     * Find child node by key
     */
    public int findChildPageId(BPlusTreeKey key) {
        int pos = findKeyPosition(key);
        return childPageIds.get(pos);
    }
    
    @Override
    public void serializeToPage(Page page) {
        byte[] data = new byte[Page.PAGE_SIZE];
        int offset = 0;
        
        // Write header information
        writeInt(data, offset, pageId); offset += PAGE_ID_SIZE;
        writeBoolean(data, offset, isLeaf); offset += BOOLEAN_SIZE;
        writeInt(data, offset, parentPageId); offset += PAGE_ID_SIZE;
        writeInt(data, offset, keys.size()); offset += INT_SIZE;
        
        // Write keys
        for (BPlusTreeKey key : keys) {
            byte[] keyBytes = key.toBytes();
            System.arraycopy(keyBytes, 0, data, offset, keyBytes.length);
            offset += keyBytes.length;
        }
        
        // Write child node page IDs
        for (int childPageId : childPageIds) {
            writeInt(data, offset, childPageId);
            offset += PAGE_ID_SIZE;
        }
        
        page.setData(data);
    }
    
    @Override
    public void deserializeFromPage(Page page) {
        byte[] data = page.getData();
        int offset = 0;
        
        // Read header information
        pageId = readInt(data, offset); offset += PAGE_ID_SIZE;
        isLeaf = readBoolean(data, offset); offset += BOOLEAN_SIZE;
        parentPageId = readInt(data, offset); offset += PAGE_ID_SIZE;
        int keyCount = readInt(data, offset); offset += INT_SIZE;
        
        // Clear existing data
        keys.clear();
        childPageIds.clear();
        
        // Read keys
        for (int i = 0; i < keyCount; i++) {
            IntegerKey key = new IntegerKey();
            key.fromBytes(data, offset);
            keys.add(key);
            offset += key.getSize();
        }
        
        // Read child node page IDs
        for (int i = 0; i <= keyCount; i++) {
            int childPageId = readInt(data, offset);
            childPageIds.add(childPageId);
            offset += PAGE_ID_SIZE;
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = HEADER_SIZE;
        for (BPlusTreeKey key : keys) {
            size += key.getSize();
        }
        size += (keys.size() + 1) * PAGE_ID_SIZE; // Child node page IDs
        return size;
    }
    
    @Override
    public void clear() {
        super.clear();
        childPageIds.clear();
    }
    
    // Helper methods
    private void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }
    
    private void writeBoolean(byte[] data, int offset, boolean value) {
        data[offset] = (byte) (value ? 1 : 0);
    }
    
    private int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }
    
    private boolean readBoolean(byte[] data, int offset) {
        return data[offset] != 0;
    }
}

/**
 * B+ Tree Leaf Node
 */
class BPlusTreeLeafNode extends BPlusTreeNode {
    private List<Integer> recordPageIds; // Record page ID list
    private int nextLeafPageId; // Next leaf node page ID
    private int prevLeafPageId; // Previous leaf node page ID
    
    public BPlusTreeLeafNode(int pageId, int maxKeys) {
        super(pageId, true, maxKeys);
        this.recordPageIds = new ArrayList<>();
        this.nextLeafPageId = -1;
        this.prevLeafPageId = -1;
    }
    
    public List<Integer> getRecordPageIds() { return recordPageIds; }
    public void setRecordPageIds(List<Integer> recordPageIds) { this.recordPageIds = recordPageIds; }
    
    public int getNextLeafPageId() { return nextLeafPageId; }
    public void setNextLeafPageId(int nextLeafPageId) { this.nextLeafPageId = nextLeafPageId; }
    
    public int getPrevLeafPageId() { return prevLeafPageId; }
    public void setPrevLeafPageId(int prevLeafPageId) { this.prevLeafPageId = prevLeafPageId; }
    
    public int getRecordCount() { return recordPageIds.size(); }
    
    public void addRecord(int recordPageId) {
        recordPageIds.add(recordPageId);
    }
    
    public void insertRecord(int index, int recordPageId) {
        recordPageIds.add(index, recordPageId);
    }
    
    public int removeRecord(int index) {
        return recordPageIds.remove(index);
    }
    
    public int getRecord(int index) {
        return recordPageIds.get(index);
    }
    
    /**
     * Find record page ID by key
     */
    public int findRecordPageId(BPlusTreeKey key) {
        int pos = findKeyPosition(key);
        if (pos < keys.size() && keys.get(pos).equals(key)) {
            return recordPageIds.get(pos);
        }
        return -1; // Not found
    }
    
    @Override
    public void serializeToPage(Page page) {
        byte[] data = new byte[Page.PAGE_SIZE];
        int offset = 0;
        
        // Write header information
        writeInt(data, offset, pageId); offset += PAGE_ID_SIZE;
        writeBoolean(data, offset, isLeaf); offset += BOOLEAN_SIZE;
        writeInt(data, offset, parentPageId); offset += PAGE_ID_SIZE;
        writeInt(data, offset, keys.size()); offset += INT_SIZE;
        
        // Write leaf node specific information
        writeInt(data, offset, nextLeafPageId); offset += PAGE_ID_SIZE;
        writeInt(data, offset, prevLeafPageId); offset += PAGE_ID_SIZE;
        
        // Write keys and record page IDs
        for (int i = 0; i < keys.size(); i++) {
            BPlusTreeKey key = keys.get(i);
            byte[] keyBytes = key.toBytes();
            
            // Check if we have enough space
            if (offset + keyBytes.length + PAGE_ID_SIZE > Page.PAGE_SIZE) {
                // Truncate the data if it exceeds page size
                break;
            }
            
            System.arraycopy(keyBytes, 0, data, offset, keyBytes.length);
            offset += keyBytes.length;
            
            writeInt(data, offset, recordPageIds.get(i));
            offset += PAGE_ID_SIZE;
        }
        
        page.setData(data);
    }
    
    @Override
    public void deserializeFromPage(Page page) {
        byte[] data = page.getData();
        int offset = 0;
        
        // Read header information
        pageId = readInt(data, offset); offset += PAGE_ID_SIZE;
        isLeaf = readBoolean(data, offset); offset += BOOLEAN_SIZE;
        parentPageId = readInt(data, offset); offset += PAGE_ID_SIZE;
        int keyCount = readInt(data, offset); offset += INT_SIZE;
        
        // Read leaf node specific information
        nextLeafPageId = readInt(data, offset); offset += PAGE_ID_SIZE;
        prevLeafPageId = readInt(data, offset); offset += PAGE_ID_SIZE;
        
        // Clear existing data
        keys.clear();
        recordPageIds.clear();
        
        // Read keys and record page IDs
        for (int i = 0; i < keyCount; i++) {
            IntegerKey key = new IntegerKey();
            key.fromBytes(data, offset);
            keys.add(key);
            offset += key.getSize();
            
            int recordPageId = readInt(data, offset);
            recordPageIds.add(recordPageId);
            offset += PAGE_ID_SIZE;
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = HEADER_SIZE + 2 * PAGE_ID_SIZE; // Additional next/prev page IDs
        for (BPlusTreeKey key : keys) {
            size += key.getSize() + PAGE_ID_SIZE; // Key + record page ID
        }
        return size;
    }
    
    @Override
    public void clear() {
        super.clear();
        recordPageIds.clear();
        nextLeafPageId = -1;
        prevLeafPageId = -1;
    }
    
    // Helper methods
    private void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }
    
    private void writeBoolean(byte[] data, int offset, boolean value) {
        data[offset] = (byte) (value ? 1 : 0);
    }
    
    private int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }
    
    private boolean readBoolean(byte[] data, int offset) {
        return data[offset] != 0;
    }
}